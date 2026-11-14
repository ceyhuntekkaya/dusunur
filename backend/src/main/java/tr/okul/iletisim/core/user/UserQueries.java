package tr.okul.iletisim.core.user;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;
import tr.okul.iletisim.core.user.UserDtos.AdminUserRow;
import tr.okul.iletisim.core.user.UserDtos.MembershipInfo;

@Component
public class UserQueries {

	record Flat(long id, String firstName, String lastName, String email, String phone, String discipline,
			String status, boolean systemAdmin, boolean passwordSet, OffsetDateTime lastLoginAt, Long membershipId,
			Long branchId, String branchName, String title, Boolean manager, Boolean printOperator,
			Boolean canCreateDmsSpace, Long printApproverId, String printApproverName) {

		AdminUserRow toRow() {
			MembershipInfo m = membershipId == null ? null
					: new MembershipInfo(membershipId, branchId, branchName, title, Boolean.TRUE.equals(manager),
							Boolean.TRUE.equals(printOperator), Boolean.TRUE.equals(canCreateDmsSpace),
							printApproverId, printApproverName);
			return new AdminUserRow(id, firstName, lastName, firstName + " " + lastName, email, phone, discipline,
					status, systemAdmin, passwordSet, lastLoginAt, m);
		}

	}

	private static final String SELECT = """
			SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.discipline, u.status, u.system_admin,
			       (u.password_hash IS NOT NULL) AS password_set, u.last_login_at,
			       m.id AS membership_id, m.branch_id, b.name AS branch_name, m.title,
			       m.is_manager AS manager, m.is_print_operator AS print_operator,
			       m.can_create_dms_space, m.print_approver_id,
			       ap.first_name || ' ' || ap.last_name AS print_approver_name
			""";

	private static final String FROM = """
			FROM app_user u
			LEFT JOIN season s ON s.status = 'ACTIVE'
			LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id
			LEFT JOIN branch b ON b.id = m.branch_id
			LEFT JOIN app_user ap ON ap.id = m.print_approver_id
			""";

	private final JdbcClient jdbc;

	public UserQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public AdminUserRow row(long id) {
		return jdbc.sql(SELECT + FROM + " WHERE u.id = :id").param("id", id).query(Flat.class).optional()
				.orElseThrow(() -> new BusinessException(CoreErrorCode.USER_NOT_FOUND)).toRow();
	}

	public PageResponse<AdminUserRow> page(String q, String status, Long branchId, Boolean assigned,
			Boolean passwordSet, int page, int size) {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		boolean hasQ = !TextNormalizer.fold(TextNormalizer.truncateQuery(q)).isEmpty();
		if (hasQ) {
			where.append(" AND u.search_key LIKE :q");
		}
		if (!"ALL".equals(status)) {
			where.append(" AND u.status = :status");
		}
		if (branchId != null) {
			where.append(" AND m.branch_id = :bid");
		}
		if (assigned != null) {
			where.append(assigned ? " AND m.id IS NOT NULL" : " AND m.id IS NULL");
		}
		if (passwordSet != null) {
			where.append(passwordSet ? " AND u.password_hash IS NOT NULL" : " AND u.password_hash IS NULL");
		}
		int s = PageResponse.clampSize(size);
		int p = Math.max(0, page);
		JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) " + FROM + where);
		JdbcClient.StatementSpec list = jdbc.sql(SELECT + FROM + where
				+ " ORDER BY u.last_name COLLATE \"tr-x-icu\", u.first_name COLLATE \"tr-x-icu\", u.id LIMIT :limit OFFSET :offset")
				.param("limit", s).param("offset", (long) p * s);
		if (hasQ) {
			count = count.param("q", TextNormalizer.likePattern(q));
			list = list.param("q", TextNormalizer.likePattern(q));
		}
		if (!"ALL".equals(status)) {
			count = count.param("status", status);
			list = list.param("status", status);
		}
		if (branchId != null) {
			count = count.param("bid", branchId);
			list = list.param("bid", branchId);
		}
		long total = count.query(Long.class).single();
		List<AdminUserRow> items = list.query(Flat.class).list().stream().map(Flat::toRow).toList();
		return new PageResponse<>(items, p, s, total);
	}

	public List<DirUser> lookup(String q, Long seasonId) {
		if (TextNormalizer.fold(TextNormalizer.truncateQuery(q)).length() < 2) {
			return List.of();
		}
		String join = seasonId != null
				? "JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid"
				: "LEFT JOIN season s ON s.status = 'ACTIVE' LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id";
		JdbcClient.StatementSpec spec = jdbc.sql("""
				SELECT u.id, u.first_name || ' ' || u.last_name AS full_name, m.title, b.name AS branch_name, u.discipline
				FROM app_user u
				""" + join + """
				 LEFT JOIN branch b ON b.id = m.branch_id
				WHERE u.status = 'ACTIVE' AND u.search_key LIKE :q
				ORDER BY u.last_name COLLATE "tr-x-icu", u.first_name COLLATE "tr-x-icu", u.id
				LIMIT 20
				""").param("q", TextNormalizer.likePattern(q));
		if (seasonId != null) {
			spec = spec.param("sid", seasonId);
		}
		return spec.query(DirUser.class).list();
	}

	public List<String> titles() {
		return jdbc.sql("SELECT title FROM (SELECT DISTINCT title FROM season_membership WHERE title IS NOT NULL) t ORDER BY title COLLATE \"tr-x-icu\"")
				.query(String.class).list();
	}

}
