package tr.okul.iletisim.core.directory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;

@Service
public class AudienceResolver {

	private final JdbcClient jdbc;

	public AudienceResolver(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public Set<Long> resolveUserIds(long seasonId, Collection<TargetRef> targets) {
		Map<TargetType, Set<Long>> ids = group(targets);
		boolean all = ids.containsKey(TargetType.ALL);
		StringBuilder sql = new StringBuilder("""
				SELECT DISTINCT u.id FROM app_user u
				JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				WHERE u.status = 'ACTIVE'
				""");
		List<String> conds = new ArrayList<>();
		if (!all) {
			if (!ids.get(TargetType.USER).isEmpty()) {
				conds.add("u.id IN (:userIds)");
			}
			if (!ids.get(TargetType.BRANCH).isEmpty()) {
				conds.add("m.branch_id IN (:branchIds)");
			}
			if (!ids.get(TargetType.GROUP).isEmpty()) {
				conds.add("u.id IN (SELECT gm.user_id FROM staff_group_member gm WHERE gm.group_id IN (:groupIds))");
			}
			if (conds.isEmpty()) {
				return Set.of();
			}
			sql.append(" AND (").append(String.join(" OR ", conds)).append(")");
		}
		JdbcClient.StatementSpec spec = jdbc.sql(sql.toString()).param("sid", seasonId);
		if (!all) {
			if (!ids.get(TargetType.USER).isEmpty()) {
				spec = spec.param("userIds", ids.get(TargetType.USER));
			}
			if (!ids.get(TargetType.BRANCH).isEmpty()) {
				spec = spec.param("branchIds", ids.get(TargetType.BRANCH));
			}
			if (!ids.get(TargetType.GROUP).isEmpty()) {
				spec = spec.param("groupIds", ids.get(TargetType.GROUP));
			}
		}
		return new LinkedHashSet<>(spec.query(Long.class).list());
	}

	@Transactional(readOnly = true)
	public void validate(UserContext me, Collection<TargetRef> targets, Set<TargetType> allowed) {
		if (targets == null || targets.isEmpty()) {
			throw new BusinessException(CoreErrorCode.TARGET_REQUIRED);
		}
		for (TargetRef t : targets) {
			if (t == null || t.type() == null) {
				throw new BusinessException(CoreErrorCode.TARGET_INVALID);
			}
		}
		boolean all = targets.stream().anyMatch(t -> t.type() == TargetType.ALL);
		List<TargetRef> effective = all ? List.of(new TargetRef(TargetType.ALL, null)) : List.copyOf(targets);
		for (TargetRef t : effective) {
			if (!allowed.contains(t.type())) {
				throw new BusinessException(CoreErrorCode.TARGET_TYPE_NOT_ALLOWED, t.type().label());
			}
		}
		if (all) {
			return;
		}
		long seasonId = me.requireMember();
		Map<TargetType, Set<Long>> ids = group(effective);
		for (TargetRef t : effective) {
			if (t.id() == null) {
				throw new BusinessException(CoreErrorCode.TARGET_INVALID);
			}
		}
		requireAll(ids.get(TargetType.USER), """
				SELECT count(*) FROM app_user u JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				WHERE u.status = 'ACTIVE' AND u.id IN (:ids)
				""", seasonId);
		requireAll(ids.get(TargetType.GROUP), "SELECT count(*) FROM staff_group WHERE archived = false AND id IN (:ids)", null);
		requireAll(ids.get(TargetType.BRANCH), "SELECT count(*) FROM branch WHERE active = true AND id IN (:ids)", null);
	}

	@Transactional(readOnly = true)
	public List<TargetView> describe(Collection<TargetRef> targets) {
		if (targets.stream().anyMatch(t -> t.type() == TargetType.ALL)) {
			return List.of(new TargetView(TargetType.ALL, null, "Tüm Kurum"));
		}
		List<TargetView> result = new ArrayList<>();
		for (TargetRef t : targets) {
			String sql = switch (t.type()) {
				case USER -> "SELECT first_name || ' ' || last_name FROM app_user WHERE id = :id";
				case GROUP -> "SELECT name FROM staff_group WHERE id = :id";
				case BRANCH -> "SELECT name FROM branch WHERE id = :id";
				case ALL -> null;
			};
			if (sql == null || t.id() == null) {
				continue;
			}
			jdbc.sql(sql).param("id", t.id()).query(String.class).optional()
					.ifPresent(name -> result.add(new TargetView(t.type(), t.id(), name)));
		}
		return result;
	}

	private void requireAll(Set<Long> ids, String sql, Long seasonId) {
		if (ids.isEmpty()) {
			return;
		}
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("ids", ids);
		if (seasonId != null) {
			spec = spec.param("sid", seasonId);
		}
		long found = spec.query(Long.class).single();
		if (found != ids.size()) {
			throw new BusinessException(CoreErrorCode.TARGET_INVALID);
		}
	}

	private static Map<TargetType, Set<Long>> group(Collection<TargetRef> targets) {
		Map<TargetType, Set<Long>> ids = new EnumMap<>(TargetType.class);
		for (TargetType type : TargetType.values()) {
			if (type != TargetType.ALL) {
				ids.put(type, new LinkedHashSet<>());
			}
		}
		for (TargetRef t : targets) {
			if (t.type() == TargetType.ALL) {
				ids.putIfAbsent(TargetType.ALL, Set.of());
			}
			else if (t.id() != null) {
				ids.get(t.type()).add(t.id());
			}
		}
		return ids;
	}

}
