package tr.okul.iletisim.calendar;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.calendar.CalendarAccess.Visible;
import tr.okul.iletisim.calendar.CalendarDtos.CalShareRow;
import tr.okul.iletisim.calendar.CalendarDtos.CalendarDto;
import tr.okul.iletisim.calendar.CalendarDtos.CalendarRequest;
import tr.okul.iletisim.calendar.CalendarDtos.ShareCreateRequest;
import tr.okul.iletisim.calendar.CalendarDtos.ShareResult;
import tr.okul.iletisim.calendar.CalendarDtos.ShareUpdateRequest;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.directory.AudienceResolver;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;

@Service
public class CalendarService {

	private static final int PERSONAL_LIMIT = 10;
	private static final int SHARE_LIMIT = 50;

	private final CalCalendarRepository calendars;
	private final CalShareRepository shares;
	private final CalendarAccess access;
	private final CalendarBootstrap bootstrap;
	private final CalendarQueries queries;
	private final CalendarEntryService entryService;
	private final AudienceResolver audience;
	private final AuditService audit;
	private final NotificationService notifications;
	private final JdbcClient jdbc;

	public CalendarService(CalCalendarRepository calendars, CalShareRepository shares, CalendarAccess access,
			CalendarBootstrap bootstrap, CalendarQueries queries, CalendarEntryService entryService,
			AudienceResolver audience, AuditService audit, NotificationService notifications, JdbcClient jdbc) {
		this.calendars = calendars;
		this.shares = shares;
		this.access = access;
		this.bootstrap = bootstrap;
		this.queries = queries;
		this.entryService = entryService;
		this.audience = audience;
		this.audit = audit;
		this.notifications = notifications;
		this.jdbc = jdbc;
	}

	@Transactional
	public List<CalendarDto> list(UserContext me) {
		me.requireMember();
		bootstrap.ensure(me);
		return queries.calendars(me);
	}

	@Transactional
	public CalendarDto createPersonal(UserContext me, CalendarRequest req) {
		me.requireMember();
		bootstrap.ensure(me);
		if (calendars.countByTypeAndOwnerId(CalendarType.PERSONAL, me.userId()) >= PERSONAL_LIMIT) {
			throw new BusinessException(CalendarErrorCode.CAL_PERSONAL_LIMIT);
		}
		CalCalendar c = new CalCalendar();
		c.setType(CalendarType.PERSONAL);
		c.setName(name(req.name()));
		c.setDescription(description(req.description()));
		c.setOwnerId(me.userId());
		c.setCreatedBy(me.userId());
		calendars.saveAndFlush(c);
		return queries.calendar(me, c.getId());
	}

	@Transactional
	public CalendarDto createResource(UserContext me, CalendarRequest req) {
		me.requireMember();
		if (!me.isManager()) {
			throw new BusinessException(CalendarErrorCode.CAL_RESOURCE_FORBIDDEN);
		}
		String name = name(req.name());
		if (calendars.existsByTypeAndNameKey(CalendarType.RESOURCE, TextNormalizer.fold(name))) {
			throw BusinessException.field(CalendarErrorCode.CAL_RESOURCE_NAME_TAKEN, "name");
		}
		CalCalendar c = new CalCalendar();
		c.setType(CalendarType.RESOURCE);
		c.setName(name);
		c.setDescription(description(req.description()));
		c.setBranchId(branch(req.branchId()));
		c.setCreatedBy(me.userId());
		save(c);
		audit.log(me.userId(), CalendarAuditAction.CAL_RESOURCE_CREATED, "CAL_CALENDAR", c.getId(), Map.of("name", name));
		return queries.calendar(me, c.getId());
	}

	@Transactional
	public CalendarDto update(UserContext me, long id, CalendarRequest req) {
		me.requireMember();
		CalCalendar c = manageable(me, id);
		String name = name(req.name());
		if (c.getType() == CalendarType.RESOURCE) {
			if (calendars.existsByTypeAndNameKeyAndIdNot(CalendarType.RESOURCE, TextNormalizer.fold(name), id)) {
				throw BusinessException.field(CalendarErrorCode.CAL_RESOURCE_NAME_TAKEN, "name");
			}
			c.setBranchId(branch(req.branchId()));
		}
		c.setName(name);
		c.setDescription(description(req.description()));
		save(c);
		if (c.getType() == CalendarType.RESOURCE) {
			audit.log(me.userId(), CalendarAuditAction.CAL_RESOURCE_UPDATED, "CAL_CALENDAR", id, Map.of("name", name));
		}
		return queries.calendar(me, id);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		me.requireMember();
		CalCalendar c = manageable(me, id);
		if (c.getType() == CalendarType.RESOURCE) {
			long future = jdbc.sql("SELECT count(*) FROM cal_entry WHERE calendar_id = :id AND start_at > now()")
					.param("id", id).query(Long.class).single();
			if (future > 0) {
				throw new BusinessException(CalendarErrorCode.CAL_RESOURCE_HAS_FUTURE, future);
			}
		}
		entryService.deleteFilesOf(id);
		calendars.delete(c);
		calendars.flush();
		if (c.getType() == CalendarType.RESOURCE) {
			audit.log(me.userId(), CalendarAuditAction.CAL_RESOURCE_DELETED, "CAL_CALENDAR", id,
					Map.of("name", c.getName()));
		}
	}

	@Transactional(readOnly = true)
	public List<CalShareRow> shares(UserContext me, long id) {
		me.requireMember();
		CalCalendar c = ownedPersonal(me, id);
		return shares.findByCalendarId(c.getId()).stream().map(this::row).toList();
	}

	@Transactional
	public ShareResult addShare(UserContext me, long id, ShareCreateRequest req) {
		me.requireMember();
		Visible v = access.require(me, id);
		CalCalendar c = v.calendar();
		if (c.getType() != CalendarType.PERSONAL) {
			throw new BusinessException(CalendarErrorCode.CAL_SHARE_NOT_PERSONAL);
		}
		if (v.access() != CalAccess.OWNER) {
			throw new BusinessException(CalendarErrorCode.CAL_MANAGE_FORBIDDEN);
		}
		TargetRef subject = req.subject();
		audience.validate(me, List.of(subject), java.util.Set.of(TargetType.USER, TargetType.GROUP));
		if (subject.type() == TargetType.USER && subject.id() == me.userId()) {
			throw new BusinessException(CalendarErrorCode.CAL_SHARE_OWNER);
		}
		CalShare existing = shares.findByCalendarIdAndSubjectTypeAndSubjectId(id, subject.type(), subject.id())
				.orElse(null);
		if (existing != null) {
			existing.setLevel(req.level());
			shares.saveAndFlush(existing);
			return new ShareResult(row(existing), false);
		}
		if (shares.countByCalendarId(id) >= SHARE_LIMIT) {
			throw new BusinessException(CalendarErrorCode.CAL_SHARE_LIMIT);
		}
		CalShare s = new CalShare();
		s.setCalendarId(id);
		s.setSubjectType(subject.type());
		s.setSubjectId(subject.id());
		s.setLevel(req.level());
		shares.saveAndFlush(s);
		if (subject.type() == TargetType.USER) {
			notifications.notify(List.of(subject.id()), "CAL_SHARED",
					me.fullName() + " '" + c.getName() + "' takvimini sizinle paylaştı", null, "/takvim");
		}
		return new ShareResult(row(s), true);
	}

	@Transactional
	public CalShareRow updateShare(UserContext me, long id, long shareId, ShareUpdateRequest req) {
		me.requireMember();
		ownedPersonal(me, id);
		CalShare s = shares.findByIdAndCalendarId(shareId, id)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
		s.setLevel(req.level());
		shares.saveAndFlush(s);
		return row(s);
	}

	@Transactional
	public void deleteShare(UserContext me, long id, long shareId) {
		me.requireMember();
		ownedPersonal(me, id);
		CalShare s = shares.findByIdAndCalendarId(shareId, id)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
		shares.delete(s);
	}

	private CalShareRow row(CalShare s) {
		var subject = audience.describe(List.of(new TargetRef(s.getSubjectType(), s.getSubjectId())));
		return new CalShareRow(s.getId(), subject.isEmpty() ? new tr.okul.iletisim.core.directory.DirectoryDtos.TargetView(
				s.getSubjectType(), s.getSubjectId(), "") : subject.get(0), s.getLevel());
	}

	private CalCalendar ownedPersonal(UserContext me, long id) {
		Visible v = access.require(me, id);
		if (v.calendar().getType() != CalendarType.PERSONAL || v.access() != CalAccess.OWNER) {
			throw new BusinessException(CalendarErrorCode.CAL_MANAGE_FORBIDDEN);
		}
		return v.calendar();
	}

	private CalCalendar manageable(UserContext me, long id) {
		Visible v = access.require(me, id);
		CalCalendar c = v.calendar();
		switch (c.getType()) {
			case INSTITUTION, BRANCH, GROUP -> throw new BusinessException(CalendarErrorCode.CAL_SYSTEM_CALENDAR);
			case PERSONAL -> {
				if (v.access() != CalAccess.OWNER) {
					throw new BusinessException(CalendarErrorCode.CAL_MANAGE_FORBIDDEN);
				}
				if (c.isDefault()) {
					throw new BusinessException(CalendarErrorCode.CAL_DEFAULT_CALENDAR);
				}
			}
			case RESOURCE -> {
				if (!me.isManager()) {
					throw new BusinessException(CalendarErrorCode.CAL_MANAGE_FORBIDDEN);
				}
			}
		}
		return c;
	}

	private Long branch(Long branchId) {
		if (branchId == null) {
			return null;
		}
		boolean exists = jdbc.sql("SELECT EXISTS (SELECT 1 FROM branch WHERE id = :id)").param("id", branchId)
				.query(Boolean.class).single();
		if (!exists) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "branchId");
		}
		return branchId;
	}

	private void save(CalCalendar c) {
		try {
			calendars.saveAndFlush(c);
		}
		catch (DataIntegrityViolationException e) {
			if (c.getType() == CalendarType.RESOURCE && String.valueOf(e.getMostSpecificCause().getMessage()).contains("ux_cal_resource")) {
				throw BusinessException.field(CalendarErrorCode.CAL_RESOURCE_NAME_TAKEN, "name");
			}
			throw e;
		}
	}

	private static String name(String raw) {
		String name = TextNormalizer.clean(raw);
		if (name == null || name.length() < 2 || name.length() > 100) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "name");
		}
		return name;
	}

	private static String description(String raw) {
		String d = raw == null ? null : raw.trim();
		return d == null || d.isEmpty() ? null : d;
	}

}
