package tr.okul.iletisim.core.season;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;
import tr.okul.iletisim.core.season.SeasonDtos.MembershipRow;
import tr.okul.iletisim.core.season.SeasonDtos.SeasonRow;

@Component
public class SeasonQueries {

	private static final String SEASON_ROW = """
			SELECT s.id, s.name, s.start_date, s.end_date, s.status, s.activated_at, s.closed_at,
			       (SELECT count(*) FROM season_membership m WHERE m.season_id = s.id)::int AS member_count
			FROM season s
			""";

	private static final String MEMBERSHIP_FROM = """
			FROM season_membership m
			JOIN app_user u ON u.id = m.user_id
			JOIN branch b ON b.id = m.branch_id
			LEFT JOIN app_user ap ON ap.id = m.print_approver_id
			""";

	private static final String MEMBERSHIP_SELECT = """
			SELECT m.id, m.user_id, u.first_name || ' ' || u.last_name AS full_name, u.email, u.status AS user_status,
			       m.branch_id, b.name AS branch_name, m.title, m.is_manager AS manager,
			       m.is_print_operator AS print_operator, m.can_create_dms_space, m.print_approver_id,
			       ap.first_name || ' ' || ap.last_name AS print_approver_name
			""";

	private final JdbcClient jdbc;

	public SeasonQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<SeasonRow> seasons() {
		return jdbc.sql(SEASON_ROW + " ORDER BY s.name DESC").query(SeasonRow.class).list();
	}

	public SeasonRow season(long id) {
		return jdbc.sql(SEASON_ROW + " WHERE s.id = :id").param("id", id).query(SeasonRow.class).optional()
				.orElseThrow(() -> new BusinessException(CoreErrorCode.SEASON_NOT_FOUND));
	}

	public MembershipRow membership(long id) {
		return jdbc.sql(MEMBERSHIP_SELECT + MEMBERSHIP_FROM + " WHERE m.id = :id").param("id", id)
				.query(MembershipRow.class).optional()
				.orElseThrow(() -> new BusinessException(CoreErrorCode.SEASON_MEMBERSHIP_NOT_FOUND));
	}

	public PageResponse<MembershipRow> memberships(long seasonId, String q, Long branchId, String filter, int page,
			int size) {
		StringBuilder where = new StringBuilder(" WHERE m.season_id = :sid");
		boolean hasQ = !TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty();
		if (hasQ) {
			where.append(" AND u.search_key LIKE :q");
		}
		if (branchId != null) {
			where.append(" AND m.branch_id = :bid");
		}
		if (filter != null) {
			switch (filter) {
				case "manager" -> where.append(" AND m.is_manager");
				case "printOperator" -> where.append(" AND m.is_print_operator");
				case "dmsCreator" -> where.append(" AND m.can_create_dms_space");
				case "noApprover" -> where.append(" AND m.print_approver_id IS NULL");
				default -> {
				}
			}
		}
		int s = PageResponse.clampSize(size);
		int p = Math.max(0, page);
		JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) " + MEMBERSHIP_FROM + where).param("sid", seasonId);
		JdbcClient.StatementSpec list = jdbc.sql(MEMBERSHIP_SELECT + MEMBERSHIP_FROM + where
				+ " ORDER BY u.last_name COLLATE \"tr-x-icu\", u.first_name COLLATE \"tr-x-icu\", m.id LIMIT :limit OFFSET :offset")
				.param("sid", seasonId).param("limit", s).param("offset", (long) p * s);
		if (hasQ) {
			count = count.param("q", TextNormalizer.likePattern(q));
			list = list.param("q", TextNormalizer.likePattern(q));
		}
		if (branchId != null) {
			count = count.param("bid", branchId);
			list = list.param("bid", branchId);
		}
		long total = count.query(Long.class).single();
		return new PageResponse<>(list.query(MembershipRow.class).list(), p, s, total);
	}

	public List<DirUser> unassigned(long seasonId, String q) {
		boolean hasQ = !TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty();
		JdbcClient.StatementSpec spec = jdbc.sql("""
				SELECT u.id, u.first_name || ' ' || u.last_name AS full_name, CAST(NULL AS text) AS title,
				       CAST(NULL AS text) AS branch_name, u.discipline
				FROM app_user u
				WHERE u.status = 'ACTIVE'
				  AND NOT EXISTS (SELECT 1 FROM season_membership m WHERE m.season_id = :sid AND m.user_id = u.id)
				""" + (hasQ ? " AND u.search_key LIKE :q" : "")
				+ " ORDER BY u.last_name COLLATE \"tr-x-icu\", u.first_name COLLATE \"tr-x-icu\", u.id LIMIT 1000")
				.param("sid", seasonId);
		if (hasQ) {
			spec = spec.param("q", TextNormalizer.likePattern(q));
		}
		return spec.query(DirUser.class).list();
	}

	public int activeMemberCount(long seasonId) {
		return jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN app_user u ON u.id = m.user_id
				WHERE m.season_id = :sid AND u.status = 'ACTIVE'
				""").param("sid", seasonId).query(Integer.class).single();
	}

	public int managerCount(long seasonId) {
		return jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN app_user u ON u.id = m.user_id
				WHERE m.season_id = :sid AND u.status = 'ACTIVE' AND m.is_manager
				""").param("sid", seasonId).query(Integer.class).single();
	}

	public List<String> branchesWithoutPrintOperator(long seasonId) {
		return jdbc.sql("""
				SELECT b.name FROM branch b
				WHERE b.active
				  AND EXISTS (SELECT 1 FROM season_membership m JOIN app_user u ON u.id = m.user_id
				              WHERE m.season_id = :sid AND m.branch_id = b.id AND u.status = 'ACTIVE')
				  AND NOT EXISTS (SELECT 1 FROM season_membership m JOIN app_user u ON u.id = m.user_id
				                  WHERE m.season_id = :sid AND m.branch_id = b.id AND u.status = 'ACTIVE'
				                    AND m.is_print_operator)
				ORDER BY b.name
				""").param("sid", seasonId).query(String.class).list();
	}

	public int droppedMemberCount(long oldSeasonId, long newSeasonId) {
		return jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN app_user u ON u.id = m.user_id
				WHERE m.season_id = :old AND u.status = 'ACTIVE'
				  AND NOT EXISTS (SELECT 1 FROM season_membership n WHERE n.season_id = :new AND n.user_id = m.user_id)
				""").param("old", oldSeasonId).param("new", newSeasonId).query(Integer.class).single();
	}

	public int withoutApproverCount(long seasonId) {
		return jdbc.sql("SELECT count(*) FROM season_membership WHERE season_id = :sid AND print_approver_id IS NULL")
				.param("sid", seasonId).query(Integer.class).single();
	}

	public void lockAllSeasons() {
		jdbc.sql("SELECT id FROM season FOR UPDATE").query(Long.class).list();
	}

}
