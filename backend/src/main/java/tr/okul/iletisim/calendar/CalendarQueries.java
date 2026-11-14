package tr.okul.iletisim.calendar;

import java.text.Collator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.calendar.CalendarDtos.CalPerson;
import tr.okul.iletisim.calendar.CalendarDtos.CalendarDto;
import tr.okul.iletisim.calendar.CalendarDtos.EntryDto;
import tr.okul.iletisim.calendar.CalendarDtos.EntryFile;

@Component
public class CalendarQueries {

	public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

	private static final List<String> PALETTE = List.of("#2453C7", "#0F9D8A", "#7C3AED", "#D97706", "#DB2777",
			"#059669", "#DC2626", "#0891B2", "#65A30D", "#9333EA");

	private record IdName(long id, String name) {
	}

	private record EntryRow(long id, long calendarId, String title, boolean allDay, OffsetDateTime startAt,
			OffsetDateTime endAt, String location, String description, String fileName, String fileContentType,
			Long fileSize, long createdById, String createdByName, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
	}

	private static final String ENTRY_SELECT = """
			SELECT e.id, e.calendar_id, e.title, e.all_day, e.start_at, e.end_at, e.location, e.description,
			       f.original_name AS file_name, f.content_type AS file_content_type, f.size_bytes AS file_size,
			       e.created_by AS created_by_id, u.first_name || ' ' || u.last_name AS created_by_name,
			       e.created_at, e.updated_at
			FROM cal_entry e
			JOIN app_user u ON u.id = e.created_by
			LEFT JOIN stored_file f ON f.id = e.file_id AND f.deleted_at IS NULL
			""";

	private final JdbcClient jdbc;
	private final CalendarAccess access;
	private final CalCalendarRepository calendars;

	public CalendarQueries(JdbcClient jdbc, CalendarAccess access, CalCalendarRepository calendars) {
		this.jdbc = jdbc;
		this.access = access;
		this.calendars = calendars;
	}

	@Transactional(readOnly = true)
	public List<CalendarDto> calendars(UserContext me) {
		return dtos(me, access.visibleCalendars(me));
	}

	@Transactional(readOnly = true)
	public CalendarDto calendar(UserContext me, long id) {
		return dtos(me, calendars.findAllById(List.of(id))).get(0);
	}

	private List<CalendarDto> dtos(UserContext me, List<CalCalendar> list) {
		Map<Long, CalAccess> accessMap = access.accessMap(me, list);
		Set<Long> branchIds = list.stream().map(CalCalendar::getBranchId).filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
		Set<Long> ownerIds = list.stream().filter(c -> c.getType() == CalendarType.PERSONAL)
				.map(CalCalendar::getOwnerId).collect(Collectors.toSet());
		Map<Long, String> branches = names("SELECT id, name FROM branch WHERE id IN (:ids)", branchIds);
		Map<Long, String> owners = names("SELECT id, first_name || ' ' || last_name AS name FROM app_user WHERE id IN (:ids)",
				ownerIds);
		Set<Long> groupIds = me.groupIds() == null ? Set.of() : me.groupIds();
		record Item(CalendarDto dto, CalendarSection section) {
		}
		List<Item> items = new ArrayList<>();
		for (CalCalendar c : list) {
			CalAccess a = accessMap.get(c.getId());
			if (a == null) {
				continue;
			}
			CalendarSection section = switch (c.getType()) {
				case INSTITUTION -> CalendarSection.INSTITUTION;
				case BRANCH -> CalendarSection.BRANCH;
				case PERSONAL -> c.getOwnerId() == me.userId() ? CalendarSection.MINE : CalendarSection.SHARED;
				case GROUP -> groupIds.contains(c.getStaffGroupId()) ? CalendarSection.GROUP : CalendarSection.OTHER_GROUP;
				case RESOURCE -> CalendarSection.RESOURCE;
			};
			boolean canManage = (c.getType() == CalendarType.PERSONAL && a == CalAccess.OWNER)
					|| (c.getType() == CalendarType.RESOURCE && me.isManager());
			String branchName = c.getBranchId() == null ? null : branches.get(c.getBranchId());
			String name = c.getType() == CalendarType.BRANCH && branchName != null ? branchName : c.getName();
			CalPerson owner = c.getType() == CalendarType.PERSONAL
					? new CalPerson(c.getOwnerId(), owners.getOrDefault(c.getOwnerId(), "")) : null;
			items.add(new Item(new CalendarDto(c.getId(), c.getType(), name, c.getDescription(), color(c), section, a,
					a != CalAccess.VIEW && !c.isReadOnly(), canManage, c.isReadOnly(), c.isDefault(), branchName, owner),
					section));
		}
		Collator collator = Collator.getInstance(Locale.forLanguageTag("tr"));
		items.sort(Comparator.<Item>comparingInt(i -> i.section().ordinal())
				.thenComparing(i -> !i.dto().isDefault())
				.thenComparing(i -> i.dto().name(), collator));
		return items.stream().map(Item::dto).toList();
	}

	static String color(CalCalendar c) {
		int idx = switch (c.getType()) {
			case INSTITUTION -> 0;
			case BRANCH -> (int) (1 + (c.getBranchId() % 2));
			default -> (int) (3 + (c.getId() % 7));
		};
		return PALETTE.get(idx);
	}

	private Map<Long, String> names(String sql, Collection<Long> ids) {
		Map<Long, String> result = new HashMap<>();
		if (ids.isEmpty()) {
			return result;
		}
		for (IdName r : jdbc.sql(sql).param("ids", ids).query(IdName.class).list()) {
			result.put(r.id(), r.name());
		}
		return result;
	}

	@Transactional(readOnly = true)
	public List<EntryDto> entries(UserContext me, OffsetDateTime from, OffsetDateTime to, Collection<Long> calendarIds) {
		if (from == null || to == null) {
			throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, from == null ? "from" : "to");
		}
		if (!to.isAfter(from)) {
			throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, "to");
		}
		if (to.isAfter(from.plusDays(100))) {
			throw new BusinessException(CalendarErrorCode.CAL_RANGE_TOO_LONG);
		}
		Map<Long, CalCalendar> visible = access.visibleCalendars(me).stream()
				.collect(Collectors.toMap(CalCalendar::getId, c -> c));
		List<Long> ids = (calendarIds == null || calendarIds.isEmpty() ? visible.keySet() : calendarIds).stream()
				.filter(visible::containsKey).distinct().toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<Long, CalAccess> accessMap = access.accessMap(me, ids.stream().map(visible::get).toList());
		return jdbc.sql(ENTRY_SELECT + " WHERE e.calendar_id IN (:ids) AND e.start_at < :to AND e.end_at > :from ORDER BY e.start_at, e.id")
				.param("ids", ids).param("from", from).param("to", to).query(EntryRow.class).list().stream()
				.map(r -> toDto(me, r, visible.get(r.calendarId()), accessMap.get(r.calendarId()))).toList();
	}

	@Transactional(readOnly = true)
	public EntryDto entry(UserContext me, long id) {
		EntryRow r = jdbc.sql(ENTRY_SELECT + " WHERE e.id = :id").param("id", id).query(EntryRow.class).optional()
				.orElse(null);
		if (r == null) {
			return null;
		}
		CalCalendar c = calendars.findById(r.calendarId()).orElse(null);
		CalAccess a = c == null ? null : access.access(me, c);
		return a == null ? null : toDto(me, r, c, a);
	}

	private EntryDto toDto(UserContext me, EntryRow r, CalCalendar c, CalAccess a) {
		OffsetDateTime start = r.startAt().atZoneSameInstant(ZONE).toOffsetDateTime();
		OffsetDateTime end = r.endAt().atZoneSameInstant(ZONE).toOffsetDateTime();
		LocalDate startDate = r.allDay() ? start.toLocalDate() : null;
		LocalDate endDate = r.allDay() ? end.toLocalDate().minusDays(1) : null;
		EntryFile file = r.fileName() == null ? null : new EntryFile(r.fileName(), r.fileContentType(), r.fileSize());
		boolean canEdit = access.canEdit(me, c, a, r.calendarId(), r.createdById());
		return new EntryDto(r.id(), r.calendarId(), r.title(), r.allDay(), start, end, startDate, endDate,
				r.location(), r.description(), file, new CalPerson(r.createdById(), r.createdByName()),
				r.createdAt(), r.updatedAt(), canEdit);
	}

}
