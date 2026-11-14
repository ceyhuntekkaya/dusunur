package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class CsvImportTest extends IntegrationTest {

	private static final String HEADER = "ad;soyad;eposta;telefon;brans;sube;unvan\r\n";

	private ResultActions upload(long admin, long season, String csv) throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "personel.csv", "text/csv",
				csv.getBytes(Charset.forName("windows-1254")));
		return mvc.perform(multipart("/api/admin/users/import").file(file).param("seasonId", String.valueOf(season))
				.with(loginAs(admin)));
	}

	@Test
	void tc6a_windows1254DosyaDogruOkunurVeKayitOlusur() throws Exception {
		long admin = data.admin();
		data.branch("Merkez Kampüs");
		long season = data.season("2026-2027", "ACTIVE");

		upload(admin, season, HEADER
				+ "Şule;Yıldız;sule.yildiz@okul.test;05321234567;Matematik;Merkez Kampüs;Öğretmen\r\n"
				+ "Çağrı;Öztürk;cagri.ozturk@okul.test;05331234567;Fizik;Merkez Kampüs;Müdür Yardımcısı\r\n")
				.andExpect(status().isOk()).andExpect(jsonPath("$.created").value(2));

		String first = jdbc.sql("SELECT first_name || ' ' || last_name FROM app_user WHERE email = 'sule.yildiz@okul.test'")
				.query(String.class).single();
		assertThat(first).isEqualTo("Şule Yıldız");
		Long members = jdbc.sql("SELECT count(*) FROM season_membership WHERE season_id = :s").param("s", season)
				.query(Long.class).single();
		assertThat(members).isEqualTo(2);
	}

	@Test
	void tc6b_hataliSatirli422DonerHicbirKayitOlusmaz() throws Exception {
		long admin = data.admin();
		data.branch("Merkez Kampüs");
		long season = data.season("2026-2027", "ACTIVE");

		upload(admin, season, HEADER
				+ "Şule;Yıldız;sule.yildiz@okul.test;05321234567;Matematik;Merkez Kampüs;Öğretmen\r\n"
				+ "Çağrı;Öztürk;gecersiz-eposta;05331234567;Fizik;Merkez Kampüs;Müdür\r\n")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("IMPORT_INVALID"))
				.andExpect(jsonPath("$.details[0]").value(startsWith("3. satır: ")));

		Long users = jdbc.sql("SELECT count(*) FROM app_user WHERE system_admin = false").query(Long.class).single();
		assertThat(users).isZero();
		Long members = jdbc.sql("SELECT count(*) FROM season_membership").query(Long.class).single();
		assertThat(members).isZero();
	}

}
