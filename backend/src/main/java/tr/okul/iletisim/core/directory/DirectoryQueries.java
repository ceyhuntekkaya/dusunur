package tr.okul.iletisim.core.directory;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.branch.BranchDtos.DirBranch;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirGroup;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;

@Component
public class DirectoryQueries {

	private static final String GROUP_COUNT = "(SELECT count(*) FROM staff_group_member gm WHERE gm.group_id = g.id)::int";

	private final JdbcClient jdbc;

	public DirectoryQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<DirUser> users(long seasonId, String q, int limit) {
		if (TextNormalizer.fold(TextNormalizer.truncateQuery(q)).length() < 2) {
			return List.of();
		}
		return jdbc.sql("""
				SELECT u.id, u.first_name || ' ' || u.last_name AS full_name, m.title, b.name AS branch_name, u.discipline
				FROM app_user u
				JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				JOIN branch b ON b.id = m.branch_id
				WHERE u.status = 'ACTIVE' AND u.search_key LIKE :q
				ORDER BY u.last_name COLLATE "tr-x-icu", u.first_name COLLATE "tr-x-icu", u.id
				LIMIT :limit
				""").param("sid", seasonId).param("q", TextNormalizer.likePattern(q)).param("limit", limit)
				.query(DirUser.class).list();
	}

	public List<DirGroup> groups(long userId, boolean mineOnly, String q, int limit) {
		String filter = TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty() ? "" : " AND g.name_key LIKE :q";
		String mine = mineOnly
				? " AND EXISTS (SELECT 1 FROM staff_group_member x WHERE x.group_id = g.id AND x.user_id = :uid)" : "";
		JdbcClient.StatementSpec spec = jdbc.sql("SELECT g.id, g.name, " + GROUP_COUNT
				+ " AS member_count FROM staff_group g WHERE g.archived = false" + mine + filter
				+ " ORDER BY g.name COLLATE \"tr-x-icu\" LIMIT :limit").param("limit", limit);
		if (mineOnly) {
			spec = spec.param("uid", userId);
		}
		if (!filter.isEmpty()) {
			spec = spec.param("q", TextNormalizer.likePattern(q));
		}
		return spec.query(DirGroup.class).list();
	}

	public List<DirGroup> myGroups(long userId) {
		return groups(userId, true, null, Integer.MAX_VALUE);
	}

	public List<DirBranch> branches(String q, int limit) {
		String filter = TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty() ? "" : " AND name_key LIKE :q";
		JdbcClient.StatementSpec spec = jdbc.sql("SELECT id, name FROM branch WHERE active = true" + filter
				+ " ORDER BY name COLLATE \"tr-x-icu\" LIMIT :limit").param("limit", limit);
		if (!filter.isEmpty()) {
			spec = spec.param("q", TextNormalizer.likePattern(q));
		}
		return spec.query(DirBranch.class).list();
	}

}
