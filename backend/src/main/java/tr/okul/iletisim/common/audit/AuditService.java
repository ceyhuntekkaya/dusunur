package tr.okul.iletisim.common.audit;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditService {

	private final JdbcClient jdbc;
	private final JsonMapper jsonMapper;

	public AuditService(JdbcClient jdbc, JsonMapper jsonMapper) {
		this.jdbc = jdbc;
		this.jsonMapper = jsonMapper;
	}

	public void log(Long actorUserId, AuditAction action, String entityType, Object entityId, Map<String, ?> detail) {
		String json = detail == null || detail.isEmpty() ? null : jsonMapper.writeValueAsString(detail);
		jdbc.sql("""
				INSERT INTO audit_log (actor_user_id, ip, action, action_label, entity_type, entity_id, detail)
				VALUES (:actor, :ip, :action, :label, :entityType, :entityId, CAST(:detail AS jsonb))
				""")
				.param("actor", actorUserId)
				.param("ip", currentIp())
				.param("action", action.name())
				.param("label", action.label())
				.param("entityType", entityType)
				.param("entityId", entityId == null ? null : String.valueOf(entityId))
				.param("detail", json)
				.update();
	}

	private static String currentIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			String ip = attrs.getRequest().getRemoteAddr();
			return ip == null ? null : ip.length() > 45 ? ip.substring(0, 45) : ip;
		}
		return null;
	}

}
