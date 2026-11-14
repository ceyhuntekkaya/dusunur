package tr.okul.iletisim.common.audit;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tr.okul.iletisim.common.audit.AuditDtos.AuditActionOption;
import tr.okul.iletisim.common.audit.AuditDtos.AuditRow;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.core.CoreErrorCode;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

	private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

	private final JdbcClient jdbc;

	public AuditController(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping
	public PageResponse<AuditRow> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long actorId,
			@RequestParam(required = false) String action,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		LocalDate end = to != null ? to : LocalDate.now(ZONE);
		LocalDate start = from != null ? from : end.minusDays(6);
		if (ChronoUnit.DAYS.between(start, end) > 366) {
			throw new BusinessException(CoreErrorCode.AUDIT_RANGE_TOO_LONG);
		}
		int pageNo = Math.max(0, page);
		int pageSize = PageResponse.clampSize(size);
		OffsetDateTime fromTs = start.atStartOfDay(ZONE).toOffsetDateTime();
		OffsetDateTime toTs = end.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
		String act = action == null || action.isBlank() ? null : action;

		String where = """
				WHERE a.created_at >= :from AND a.created_at < :to
				  AND (CAST(:actorId AS bigint) IS NULL OR a.actor_user_id = CAST(:actorId AS bigint))
				  AND (CAST(:action AS varchar) IS NULL OR a.action = CAST(:action AS varchar))
				""";
		long total = jdbc.sql("SELECT count(*) FROM audit_log a " + where)
				.param("from", fromTs).param("to", toTs).param("actorId", actorId).param("action", act)
				.query(Long.class).single();
		List<AuditRow> items = jdbc.sql("""
				SELECT a.id, a.created_at, a.actor_user_id AS actor_id,
				       CASE WHEN u.id IS NULL THEN NULL ELSE u.first_name || ' ' || u.last_name END AS actor_name,
				       a.ip, a.action, a.action_label, a.entity_type, a.entity_id, a.detail::text AS detail
				FROM audit_log a LEFT JOIN app_user u ON u.id = a.actor_user_id
				""" + where + " ORDER BY a.created_at DESC, a.id DESC LIMIT :limit OFFSET :offset")
				.param("from", fromTs).param("to", toTs).param("actorId", actorId).param("action", act)
				.param("limit", pageSize).param("offset", (long) pageNo * pageSize)
				.query(AuditRow.class).list();
		return new PageResponse<>(items, pageNo, pageSize, total);
	}

	@GetMapping("/actions")
	public List<AuditActionOption> actions() {
		return jdbc.sql("SELECT DISTINCT action AS code, action_label AS label FROM audit_log ORDER BY action_label")
				.query(AuditActionOption.class).list();
	}

}
