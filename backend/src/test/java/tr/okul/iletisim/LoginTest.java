package tr.okul.iletisim;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class LoginTest extends IntegrationTest {

	private ResultActions login(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").with(csrf()).contentType(APPLICATION_JSON)
				.content(json(Map.of("email", email, "password", password))));
	}

	private long memberInActiveSeason(String name) {
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long id = data.user(name);
		data.member(season, id, branch, null);
		return id;
	}

	@Test
	void tc1a_dogruBilgilerleGirisMeResponseDoner() throws Exception {
		long id = memberInActiveSeason("Ayse");
		login(data.email(id), TestData.PASSWORD).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.firstName").value("Ayse"))
				.andExpect(jsonPath("$.season.name").value("2026-2027"))
				.andExpect(jsonPath("$.membership.branchName").value("Merkez"))
				.andExpect(jsonPath("$.permissions.member").value(true));
	}

	@Test
	void tc1b_besinciYanlisDenemedeKilitlenirVeDogruSifreDeKilitliKalir() throws Exception {
		long id = memberInActiveSeason("Ayse");
		String email = data.email(id);
		for (int i = 1; i <= 4; i++) {
			login(email, "Yanlis1234").andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
		}
		login(email, "Yanlis1234").andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
		login(email, TestData.PASSWORD).andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_LOCKED"));
	}

	@Test
	void tc1c_pasifKullaniciGirisYapamaz() throws Exception {
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long id = data.user("Pasif", "pasif@okul.test", "PASSIVE", false);
		data.member(season, id, branch, null);
		login("pasif@okul.test", TestData.PASSWORD).andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_PASSIVE"));
	}

	@Test
	void tc1d_aktifYildaAtanmamisKullaniciGirisYapamaz() throws Exception {
		data.season("2026-2027", "ACTIVE");
		long id = data.user("Atanmamis");
		login(data.email(id), TestData.PASSWORD).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_NO_SEASON_MEMBERSHIP"));
	}

	@Test
	void tc1e_yilaAtanmamisSistemYoneticisiGirisYapar() throws Exception {
		data.season("2026-2027", "ACTIVE");
		long id = data.admin();
		login(data.email(id), TestData.PASSWORD).andExpect(status().isOk())
				.andExpect(jsonPath("$.systemAdmin").value(true));
	}

	@Test
	void tc2a_atamasiSilinenKullaniciSonrakiIstekte403Alir() throws Exception {
		long id = memberInActiveSeason("Ayse");
		mvc.perform(get("/api/auth/me").with(loginAs(id))).andExpect(status().isOk());
		jdbc.sql("DELETE FROM season_membership WHERE user_id = :u").param("u", id).update();
		mvc.perform(get("/api/auth/me").with(loginAs(id))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AUTH_NO_SEASON_MEMBERSHIP"));
	}

	@Test
	void tc2b_kvkkOnaylamamisKullaniciNotificationsIcin403MeIcin200Alir() throws Exception {
		long id = memberInActiveSeason("Ayse");
		jdbc.sql("UPDATE app_user SET kvkk_version = 0 WHERE id = :u").param("u", id).update();
		mvc.perform(get("/api/notifications").with(loginAs(id))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("KVKK_REQUIRED"));
		mvc.perform(get("/api/auth/me").with(loginAs(id))).andExpect(status().isOk());
	}

}
