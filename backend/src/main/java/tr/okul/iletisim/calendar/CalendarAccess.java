package tr.okul.iletisim.calendar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;

@Component
public class CalendarAccess {

	public record Visible(CalCalendar calendar, CalAccess access) {
	}

	private record ShareLevel(long calendarId, int level) {
	}

	private final JdbcClient jdbc;
	private final CalCalendarRepository calendars;

	public CalendarAccess(JdbcClient jdbc, CalCalendarRepository calendars) {
		this.jdbc = jdbc;
		this.calendars = calendars;
	}

	@Transactional(readOnly = true)
	public List<CalCalendar> visibleCalendars(UserContext me) {
		Set<Long> groupIds = groups(me);
		StringBuilder sql = new StringBuilder("""
				SELECT c.id FROM cal_calendar c
				WHERE c.type IN ('INSTITUTION','BRANCH','RESOURCE')
				   OR (c.type = 'PERSONAL' AND (c.owner_id = :uid
				        OR EXISTS (SELECT 1 FROM cal_share s WHERE s.calendar_id = c.id AND
				                   ((s.subject_type = 'USER' AND s.subject_id = :uid)
				""");
		if (!groupIds.isEmpty()) {
			sql.append(" OR (s.subject_type = 'GROUP' AND s.subject_id IN (:groupIds))");
		}
		sql.append("))))");
		if (me.isManager()) {
			sql.append(" OR c.type = 'GROUP'");
		}
		else if (!groupIds.isEmpty()) {
			sql.append(" OR (c.type = 'GROUP' AND c.staff_group_id IN (:groupIds))");
		}
		JdbcClient.StatementSpec spec = jdbc.sql(sql.toString()).param("uid", me.userId());
		if (sql.indexOf(":groupIds") >= 0) {
			spec = spec.param("groupIds", groupIds);
		}
		return calendars.findAllById(spec.query(Long.class).list());
	}

	@Transactional(readOnly = true)
	public CalAccess access(UserContext me, CalCalendar calendar) {
		return accessMap(me, List.of(calendar)).get(calendar.getId());
	}

	@Transactional(readOnly = true)
	public Map<Long, CalAccess> accessMap(UserContext me, Collection<CalCalendar> list) {
		Set<Long> groupIds = groups(me);
		Map<Long, CalAccess> result = new HashMap<>();
		List<Long> sharedCandidates = new ArrayList<>();
		for (CalCalendar c : list) {
			switch (c.getType()) {
				case INSTITUTION, BRANCH -> result.put(c.getId(), me.isManager() ? CalAccess.EDIT_ALL : CalAccess.VIEW);
				case RESOURCE -> result.put(c.getId(), me.isManager() ? CalAccess.EDIT_ALL : CalAccess.CREATE);
				case GROUP -> {
					boolean member = groupIds.contains(c.getStaffGroupId());
					if (me.isManager() || member) {
						result.put(c.getId(), c.isReadOnly() ? CalAccess.VIEW
								: me.isManager() ? CalAccess.EDIT_ALL : CalAccess.CREATE);
					}
				}
				case PERSONAL -> {
					if (c.getOwnerId() != null && c.getOwnerId() == me.userId()) {
						result.put(c.getId(), CalAccess.OWNER);
					}
					else {
						sharedCandidates.add(c.getId());
					}
				}
			}
		}
		if (!sharedCandidates.isEmpty()) {
			String groupCond = groupIds.isEmpty() ? "" : " OR (s.subject_type = 'GROUP' AND s.subject_id IN (:groupIds))";
			JdbcClient.StatementSpec spec = jdbc.sql("""
					SELECT s.calendar_id, max(s.level) AS level FROM cal_share s
					WHERE s.calendar_id IN (:ids)
					  AND ((s.subject_type = 'USER' AND s.subject_id = :uid)%s)
					GROUP BY s.calendar_id
					""".formatted(groupCond)).param("ids", sharedCandidates).param("uid", me.userId());
			if (!groupIds.isEmpty()) {
				spec = spec.param("groupIds", groupIds);
			}
			for (ShareLevel s : spec.query(ShareLevel.class).list()) {
				result.put(s.calendarId(), CalShareLevel.ofRank(s.level()) == CalShareLevel.EDIT
						? CalAccess.EDIT_ALL : CalAccess.VIEW);
			}
		}
		return result;
	}

	@Transactional(readOnly = true)
	public Visible find(UserContext me, long calendarId) {
		CalCalendar c = calendars.findById(calendarId).orElse(null);
		if (c == null) {
			return null;
		}
		CalAccess a = access(me, c);
		return a == null ? null : new Visible(c, a);
	}

	@Transactional(readOnly = true)
	public Visible require(UserContext me, long calendarId) {
		Visible v = find(me, calendarId);
		if (v == null) {
			throw new BusinessException(CalendarErrorCode.CAL_CALENDAR_NOT_FOUND);
		}
		return v;
	}

	public boolean canEditEntry(UserContext me, CalCalendar calendar, CalAccess access, CalEntry entry) {
		return canEdit(me, calendar, access, entry.getCalendarId(), entry.getCreatedBy());
	}

	public boolean canEdit(UserContext me, CalCalendar calendar, CalAccess access, long entryCalendarId,
			long createdBy) {
		if (calendar.isReadOnly() || access == null || calendar.getId() != entryCalendarId) {
			return false;
		}
		return access == CalAccess.OWNER || access == CalAccess.EDIT_ALL
				|| (access == CalAccess.CREATE && createdBy == me.userId());
	}

	public boolean canEditEntry(UserContext me, CalCalendar calendar, CalEntry entry) {
		return canEditEntry(me, calendar, access(me, calendar), entry);
	}

	private static Set<Long> groups(UserContext me) {
		return me.groupIds() == null ? Set.of() : me.groupIds();
	}

}
