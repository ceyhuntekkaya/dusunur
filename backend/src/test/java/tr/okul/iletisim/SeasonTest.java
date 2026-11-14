package tr.okul.iletisim;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SeasonTest extends IntegrationTest {

	private String statusOf(long seasonId) {
		return jdbc.sql("SELECT status FROM season WHERE id = :id").param("id", seasonId).query(String.class).single();
	}

	@Test
	void tc3a_taslakYilAktiflestirilirEskiYilKapanir() throws Exception {
		long admin = data.admin();
		long branch = data.branch("Merkez");
		long u1 = data.user("Ayse");
		long u2 = data.user("Mehmet");
		long old = data.season("2025-2026", "ACTIVE");
		data.member(old, u1, branch, null);
		data.member(old, u2, branch, null);
		long draft = data.season("2026-2027", "DRAFT");
		data.member(draft, u1, branch, null);
		data.member(draft, u2, branch, null);

		mvc.perform(post("/api/admin/seasons/" + draft + "/activate").with(loginAs(admin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

		assertThat(statusOf(draft)).isEqualTo("ACTIVE");
		assertThat(statusOf(old)).isEqualTo("CLOSED");
	}

	@Test
	void tc3b_ikinciTaslakOlusturmaReddedilir() throws Exception {
		long admin = data.admin();
		data.season("2025-2026", "ACTIVE");
		data.season("2026-2027", "DRAFT");
		mvc.perform(post("/api/admin/seasons").with(loginAs(admin)).contentType(APPLICATION_JSON)
				.content(json(Map.of("name", "2027-2028", "startDate", "2027-09-01", "endDate", "2028-06-30"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SEASON_DRAFT_EXISTS"));
	}

	@Test
	void tc3c_bosYilAktiflestirilemez() throws Exception {
		long admin = data.admin();
		long draft = data.season("2026-2027", "DRAFT");
		mvc.perform(post("/api/admin/seasons/" + draft + "/activate").with(loginAs(admin)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SEASON_ACTIVATION_BLOCKED"));
		assertThat(statusOf(draft)).isEqualTo("DRAFT");
	}

	@Test
	void tc4a_devralansizPasiflestirmeReddedilir() throws Exception {
		long admin = data.admin();
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long approver = data.user("Onaylayici");
		long a = data.user("Ali");
		long b = data.user("Veli");
		data.member(season, approver, branch, null);
		data.member(season, a, branch, approver);
		data.member(season, b, branch, approver);

		mvc.perform(post("/api/admin/users/" + approver + "/deactivate").with(loginAs(admin))
				.contentType(APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("USER_SUCCESSOR_REQUIRED"));
	}

	@Test
	void tc4b_devralanlaPasiflestirmeOnaylayicilariDevreder() throws Exception {
		long admin = data.admin();
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long approver = data.user("Onaylayici");
		long successor = data.user("Devralan");
		long a = data.user("Ali");
		long b = data.user("Veli");
		data.member(season, approver, branch, null);
		data.member(season, successor, branch, null);
		data.member(season, a, branch, approver);
		data.member(season, b, branch, approver);

		mvc.perform(post("/api/admin/users/" + approver + "/deactivate").with(loginAs(admin))
				.contentType(APPLICATION_JSON).content(json(Map.of("successorUserId", successor))))
				.andExpect(status().isNoContent());

		Long count = jdbc.sql("SELECT count(*) FROM season_membership WHERE print_approver_id = :s AND user_id IN (:a, :b)")
				.param("s", successor).param("a", a).param("b", b).query(Long.class).single();
		assertThat(count).isEqualTo(2);

		mvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("email", data.email(approver), "password", TestData.PASSWORD))))
				.andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_PASSIVE"));
	}

}
