package tr.okul.iletisim.dms;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;

@Component
public class DmsAccess {

	private record LevelRow(long spaceId, int level) {
	}

	private final JdbcClient jdbc;

	public DmsAccess(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public DmsLevel level(UserContext me, long spaceId) {
		return levels(me, spaceId).get(spaceId);
	}

	@Transactional(readOnly = true)
	public Map<Long, DmsLevel> accessibleSpaceLevels(UserContext me) {
		return levels(me, null);
	}

	@Transactional(readOnly = true)
	public DmsLevel require(UserContext me, long spaceId, DmsLevel min) {
		DmsLevel current = level(me, spaceId);
		if (current == null) {
			throw new BusinessException(DmsErrorCode.DMS_SPACE_NOT_FOUND);
		}
		if (!current.atLeast(min)) {
			if (min == DmsLevel.MANAGE) {
				throw new BusinessException(DmsErrorCode.DMS_MANAGE_FORBIDDEN);
			}
			throw new BusinessException(DmsErrorCode.DMS_EDIT_FORBIDDEN, current.label());
		}
		return current;
	}

	private Map<Long, DmsLevel> levels(UserContext me, Long spaceId) {
		StringBuilder match = new StringBuilder("(g.subject_type = 'USER' AND g.subject_id = :uid) OR (g.subject_type = 'ALL')");
		boolean groups = me.groupIds() != null && !me.groupIds().isEmpty();
		boolean branch = me.membership() != null;
		if (groups) {
			match.append(" OR (g.subject_type = 'GROUP' AND g.subject_id IN (:groupIds))");
		}
		if (branch) {
			match.append(" OR (g.subject_type = 'BRANCH' AND g.subject_id = :branchId)");
		}
		String sql = """
				SELECT s.id AS space_id,
				       CASE WHEN s.owner_id = :uid THEN 3 ELSE COALESCE(max(g.level), 0) END AS level
				FROM dms_space s
				LEFT JOIN dms_space_grant g ON g.space_id = s.id AND (%s)
				WHERE %s
				GROUP BY s.id, s.owner_id
				HAVING s.owner_id = :uid OR max(g.level) IS NOT NULL
				""".formatted(match, spaceId == null ? "true" : "s.id = :spaceId");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("uid", me.userId());
		if (groups) {
			spec = spec.param("groupIds", me.groupIds());
		}
		if (branch) {
			spec = spec.param("branchId", me.membership().branchId());
		}
		if (spaceId != null) {
			spec = spec.param("spaceId", spaceId);
		}
		Map<Long, DmsLevel> result = new HashMap<>();
		for (LevelRow r : spec.query(LevelRow.class).list()) {
			result.put(r.spaceId(), DmsLevel.ofRank(r.level()));
		}
		return result;
	}

}
