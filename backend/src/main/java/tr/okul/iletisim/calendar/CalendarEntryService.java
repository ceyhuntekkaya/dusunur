package tr.okul.iletisim.calendar;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.calendar.CalendarAccess.Visible;
import tr.okul.iletisim.calendar.CalendarDtos.EntryDto;
import tr.okul.iletisim.calendar.CalendarDtos.EntryInput;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;

@Service
public class CalendarEntryService {

	private static final Locale TR = Locale.forLanguageTag("tr");
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", TR);
	private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("d MMM EEE", TR);
	private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", TR);
	private static final String ANOTHER = "başka bir rezervasyon";

	private record Times(OffsetDateTime start, OffsetDateTime end) {
	}

	private record Conflict(String title, boolean allDay, OffsetDateTime startAt, OffsetDateTime endAt, String createdByName) {
	}

	private final CalEntryRepository entries;
	private final CalendarAccess access;
	private final CalendarQueries queries;
	private final FileStorageService storage;
	private final StoredFileRepository storedFiles;
	private final AuditService audit;
	private final JdbcClient jdbc;

	public CalendarEntryService(CalEntryRepository entries, CalendarAccess access, CalendarQueries queries,
			FileStorageService storage, StoredFileRepository storedFiles, AuditService audit, JdbcClient jdbc) {
		this.entries = entries;
		this.access = access;
		this.queries = queries;
		this.storage = storage;
		this.storedFiles = storedFiles;
		this.audit = audit;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<EntryDto> list(UserContext me, OffsetDateTime from, OffsetDateTime to, List<Long> calendarIds) {
		me.requireMember();
		return queries.entries(me, from, to, calendarIds);
	}

	@Transactional(readOnly = true)
	public EntryDto get(UserContext me, long id) {
		me.requireMember();
		EntryDto dto = queries.entry(me, id);
		if (dto == null) {
			throw new BusinessException(CalendarErrorCode.CAL_ENTRY_NOT_FOUND);
		}
		return dto;
	}

	@Transactional
	public EntryDto create(UserContext me, EntryInput in) {
		me.requireMember();
		if (in.calendarId() == null) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "calendarId");
		}
		Visible v = access.require(me, in.calendarId());
		CalCalendar cal = v.calendar();
		if (cal.isReadOnly()) {
			throw new BusinessException(CalendarErrorCode.CAL_READ_ONLY);
		}
		if (v.access() == CalAccess.VIEW) {
			throw new BusinessException(CalendarErrorCode.CAL_CREATE_FORBIDDEN);
		}
		String title = title(in.title());
		Times t = times(in);
		boolean resource = cal.getType() == CalendarType.RESOURCE;
		if (resource) {
			checkConflict(cal.getId(), t, null);
		}
		CalEntry e = new CalEntry();
		e.setCalendarId(cal.getId());
		e.setResource(resource);
		e.setTitle(title);
		e.setAllDay(in.allDay());
		e.setStartAt(t.start());
		e.setEndAt(t.end());
		e.setLocation(text(in.location()));
		e.setDescription(text(in.description()));
		e.setCreatedBy(me.userId());
		e.setUpdatedBy(me.userId());
		if (in.fileId() != null) {
			e.setFileId(storage.attach(in.fileId(), me, FilePurpose.CALENDAR).getId());
		}
		save(e, resource);
		return queries.entry(me, e.getId());
	}

	@Transactional
	public EntryDto update(UserContext me, long id, EntryInput in) {
		me.requireMember();
		CalEntry e = entry(id);
		Visible v = visible(me, e);
		if (!access.canEditEntry(me, v.calendar(), v.access(), e)) {
			throw new BusinessException(CalendarErrorCode.CAL_EDIT_FORBIDDEN);
		}
		String title = title(in.title());
		Times t = times(in);
		if (e.isResource()) {
			checkConflict(e.getCalendarId(), t, e.getId());
		}
		e.setTitle(title);
		e.setAllDay(in.allDay());
		e.setStartAt(t.start());
		e.setEndAt(t.end());
		e.setLocation(text(in.location()));
		e.setDescription(text(in.description()));
		e.setUpdatedBy(me.userId());
		UUID old = e.getFileId();
		if (in.fileId() != null) {
			if (!in.fileId().equals(old)) {
				e.setFileId(storage.attach(in.fileId(), me, FilePurpose.CALENDAR).getId());
				deleteFile(old);
			}
		}
		else if (Boolean.TRUE.equals(in.removeFile()) && old != null) {
			e.setFileId(null);
			deleteFile(old);
		}
		save(e, e.isResource());
		return queries.entry(me, id);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		me.requireMember();
		CalEntry e = entry(id);
		Visible v = visible(me, e);
		if (!access.canEditEntry(me, v.calendar(), v.access(), e)) {
			throw new BusinessException(CalendarErrorCode.CAL_EDIT_FORBIDDEN);
		}
		if (e.getCreatedBy() != me.userId()) {
			audit.log(me.userId(), CalendarAuditAction.CAL_ENTRY_DELETED, "CAL_ENTRY", id,
					Map.of("calendarId", e.getCalendarId(), "title", e.getTitle(), "createdBy", e.getCreatedBy()));
		}
		UUID file = e.getFileId();
		entries.delete(e);
		entries.flush();
		deleteFile(file);
	}

	@Transactional(readOnly = true)
	public StoredFile file(UserContext me, long id) {
		me.requireMember();
		CalEntry e = entry(id);
		visible(me, e);
		if (e.getFileId() == null) {
			throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
		}
		return storage.get(e.getFileId());
	}

	@Transactional
	public void deleteFilesOf(long calendarId) {
		for (CalEntry e : entries.findByCalendarId(calendarId)) {
			deleteFile(e.getFileId());
		}
	}

	private void deleteFile(UUID fileId) {
		if (fileId != null) {
			storedFiles.findById(fileId).ifPresent(storage::delete);
		}
	}

	private CalEntry entry(long id) {
		return entries.findById(id).orElseThrow(() -> new BusinessException(CalendarErrorCode.CAL_ENTRY_NOT_FOUND));
	}

	private Visible visible(UserContext me, CalEntry e) {
		Visible v = access.find(me, e.getCalendarId());
		if (v == null) {
			throw new BusinessException(CalendarErrorCode.CAL_ENTRY_NOT_FOUND);
		}
		return v;
	}

	private void save(CalEntry e, boolean resource) {
		try {
			entries.saveAndFlush(e);
		}
		catch (DataIntegrityViolationException ex) {
			if (resource && String.valueOf(ex.getMostSpecificCause().getMessage()).contains("ex_cal_resource_overlap")) {
				throw new BusinessException(CalendarErrorCode.CAL_RESOURCE_CONFLICT, ANOTHER, ANOTHER, ANOTHER);
			}
			throw ex;
		}
	}

	private void checkConflict(long calendarId, Times t, Long selfId) {
		Conflict c = jdbc.sql("""
				SELECT e.title, e.all_day, e.start_at, e.end_at, u.first_name || ' ' || u.last_name AS created_by_name
				FROM cal_entry e JOIN app_user u ON u.id = e.created_by
				WHERE e.calendar_id = :cid AND e.start_at < :end AND e.end_at > :start
				  AND (CAST(:self AS bigint) IS NULL OR e.id <> CAST(:self AS bigint))
				ORDER BY e.start_at LIMIT 1
				""").param("cid", calendarId).param("start", t.start()).param("end", t.end()).param("self", selfId)
				.query(Conflict.class).optional().orElse(null);
		if (c != null) {
			throw new BusinessException(CalendarErrorCode.CAL_RESOURCE_CONFLICT, c.title(), c.createdByName(), describe(c));
		}
	}

	private static String describe(Conflict c) {
		OffsetDateTime s = c.startAt().atZoneSameInstant(CalendarQueries.ZONE).toOffsetDateTime();
		OffsetDateTime e = c.endAt().atZoneSameInstant(CalendarQueries.ZONE).toOffsetDateTime();
		if (c.allDay()) {
			LocalDate last = e.toLocalDate().minusDays(1);
			return last.equals(s.toLocalDate()) ? DAY.format(s) + " (tüm gün)"
					: DAY.format(s) + " – " + DAY.format(last) + " (tüm gün)";
		}
		if (s.toLocalDate().equals(e.toLocalDate())) {
			return DAY_NAME.format(s) + " " + HOUR.format(s) + "–" + HOUR.format(e);
		}
		return DAY_NAME.format(s) + " " + HOUR.format(s) + " – " + DAY_NAME.format(e) + " " + HOUR.format(e);
	}

	private static Times times(EntryInput in) {
		OffsetDateTime start;
		OffsetDateTime end;
		if (in.allDay()) {
			if (in.startDate() == null || in.endDate() == null) {
				throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "startDate");
			}
			if (in.endDate().isBefore(in.startDate())) {
				throw new BusinessException(CalendarErrorCode.CAL_END_BEFORE_START);
			}
			start = in.startDate().atStartOfDay(CalendarQueries.ZONE).toOffsetDateTime();
			end = in.endDate().plusDays(1).atStartOfDay(CalendarQueries.ZONE).toOffsetDateTime();
		}
		else {
			if (in.startAt() == null || in.endAt() == null) {
				throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "startAt");
			}
			start = in.startAt().truncatedTo(ChronoUnit.MINUTES);
			end = in.endAt().truncatedTo(ChronoUnit.MINUTES);
			if (!end.isAfter(start)) {
				throw new BusinessException(CalendarErrorCode.CAL_END_BEFORE_START);
			}
		}
		if (end.isAfter(start.plusDays(31))) {
			throw new BusinessException(CalendarErrorCode.CAL_DURATION_TOO_LONG);
		}
		LocalDate today = LocalDate.now(CalendarQueries.ZONE);
		LocalDate first = start.atZoneSameInstant(CalendarQueries.ZONE).toLocalDate();
		LocalDate last = end.minusMinutes(1).atZoneSameInstant(CalendarQueries.ZONE).toLocalDate();
		if (first.isBefore(today.minusYears(5)) || last.isAfter(today.plusYears(5))) {
			throw new BusinessException(CalendarErrorCode.CAL_DATE_OUT_OF_RANGE);
		}
		return new Times(start, end);
	}

	private static String title(String raw) {
		String t = TextNormalizer.clean(raw);
		if (t == null || t.length() < 2 || t.length() > 150) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "title");
		}
		return t;
	}

	private static String text(String raw) {
		String t = raw == null ? null : raw.trim();
		return t == null || t.isEmpty() ? null : t;
	}

}
