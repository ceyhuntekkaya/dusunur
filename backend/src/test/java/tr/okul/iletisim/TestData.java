package tr.okul.iletisim;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

class TestData {

	static final String PASSWORD = "Sifre1234";

	private final JdbcClient jdbc;
	private final PasswordEncoder encoder;
	private int seq = 0;

	TestData(JdbcClient jdbc, PasswordEncoder encoder) {
		this.jdbc = jdbc;
		this.encoder = encoder;
	}

	void clean() {
		jdbc.sql("TRUNCATE TABLE app_user, season, branch, audit_log, spring_session RESTART IDENTITY CASCADE").update();
	}

	long branch(String name) {
		return jdbc.sql("INSERT INTO branch (name, name_key) VALUES (:n, :k) RETURNING id")
				.param("n", name).param("k", tr.okul.iletisim.common.util.TextNormalizer.fold(name)).query(Long.class).single();
	}

	long user(String firstName, String email, String status, boolean systemAdmin) {
		return jdbc.sql("""
				INSERT INTO app_user (email, password_hash, first_name, last_name, search_key, status, system_admin, kvkk_version)
				VALUES (:e, :p, :f, 'Test', :s, :st, :a, 1) RETURNING id
				""").param("e", email).param("p", encoder.encode(PASSWORD)).param("f", firstName)
				.param("s", firstName + " test " + email).param("st", status).param("a", systemAdmin)
				.query(Long.class).single();
	}

	long user(String firstName) {
		return user(firstName, firstName.toLowerCase() + (++seq) + "@okul.test", "ACTIVE", false);
	}

	long admin() {
		return user("Yonetici", "admin" + (++seq) + "@okul.test", "ACTIVE", true);
	}

	String email(long userId) {
		return jdbc.sql("SELECT email FROM app_user WHERE id = :id").param("id", userId).query(String.class).single();
	}

	long season(String name, String status) {
		int year = Integer.parseInt(name.substring(0, 4));
		return jdbc.sql("""
				INSERT INTO season (name, start_date, end_date, status) VALUES (:n, :s, :e, :st) RETURNING id
				""").param("n", name).param("s", LocalDate.of(year, 9, 1)).param("e", LocalDate.of(year + 1, 6, 30))
				.param("st", status).query(Long.class).single();
	}

	long member(long seasonId, long userId, long branchId, Long approverId) {
		return jdbc.sql("""
				INSERT INTO season_membership (season_id, user_id, branch_id, print_approver_id)
				VALUES (:s, :u, :b, :a) RETURNING id
				""").param("s", seasonId).param("u", userId).param("b", branchId)
				.param("a", approverId, java.sql.Types.BIGINT).query(Long.class).single();
	}

	long group(String name, long createdBy, long... members) {
		long id = jdbc.sql("INSERT INTO staff_group (name, name_key, created_by) VALUES (:n, :k, :c) RETURNING id")
				.param("n", name).param("k", name.toLowerCase()).param("c", createdBy).query(Long.class).single();
		for (long m : members) {
			jdbc.sql("INSERT INTO staff_group_member (group_id, user_id) VALUES (:g, :u)").param("g", id)
					.param("u", m).update();
		}
		return id;
	}

}
