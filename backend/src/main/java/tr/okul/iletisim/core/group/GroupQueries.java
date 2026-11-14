package tr.okul.iletisim.core.group;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.group.GroupDtos.GroupDetail;
import tr.okul.iletisim.core.group.GroupDtos.GroupMember;
import tr.okul.iletisim.core.group.GroupDtos.GroupRow;

@Component
public class GroupQueries {

	private static final String ROW = """
			SELECT g.id, g.name, g.description, g.archived, g.created_at,
			       (SELECT count(*) FROM staff_group_member gm WHERE gm.group_id = g.id)::int AS member_count
			FROM staff_group g
			""";

	private final JdbcClient jdbc;

	public GroupQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<GroupRow> list(String q, boolean archived) {
		boolean filter = !TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty();
		JdbcClient.StatementSpec spec = jdbc.sql(ROW + " WHERE g.archived = :archived"
				+ (filter ? " AND g.name_key LIKE :q" : "") + " ORDER BY g.name COLLATE \"tr-x-icu\"")
				.param("archived", archived);
		if (filter) {
			spec = spec.param("q", TextNormalizer.likePattern(q));
		}
		return spec.query(GroupRow.class).list();
	}

	public GroupDetail detail(long id) {
		GroupRow r = jdbc.sql(ROW + " WHERE g.id = :id").param("id", id).query(GroupRow.class).optional()
				.orElseThrow(() -> new BusinessException(CoreErrorCode.GROUP_NOT_FOUND));
		List<GroupMember> members = jdbc.sql("""
				SELECT u.id AS user_id, u.first_name || ' ' || u.last_name AS full_name, m.title, b.name AS branch_name, u.status
				FROM staff_group_member gm
				JOIN app_user u ON u.id = gm.user_id
				LEFT JOIN season s ON s.status = 'ACTIVE'
				LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id
				LEFT JOIN branch b ON b.id = m.branch_id
				WHERE gm.group_id = :id
				ORDER BY u.last_name COLLATE "tr-x-icu", u.first_name COLLATE "tr-x-icu", u.id
				""").param("id", id).query(GroupMember.class).list();
		return new GroupDetail(r.id(), r.name(), r.description(), r.memberCount(), r.archived(), r.createdAt(),
				members);
	}

	public int countActiveUsers(java.util.Collection<Long> ids) {
		if (ids.isEmpty()) {
			return 0;
		}
		return jdbc.sql("SELECT count(*) FROM app_user WHERE status = 'ACTIVE' AND id IN (:ids)").param("ids", ids)
				.query(Integer.class).single();
	}

}
