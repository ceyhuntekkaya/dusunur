package tr.okul.iletisim;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FeedTest extends IntegrationTest {

	private long newPost(long season, long author, String target, Long targetId, boolean important) {
		long id = jdbc.sql("INSERT INTO feed_post (season_id, author_id, content, important) VALUES (:s, :a, 'metin', :i) RETURNING id")
				.param("s", season).param("a", author).param("i", important).query(Long.class).single();
		jdbc.sql("INSERT INTO feed_post_target (post_id, target_type, target_id) VALUES (:p, :t, :i)")
				.param("p", id).param("t", target).param("i", targetId, java.sql.Types.BIGINT).update();
		return id;
	}

	private void assertVisible(long viewer, long postId, boolean visible) throws Exception {
		var r = mvc.perform(get("/api/feed/posts/" + postId).with(loginAs(viewer)));
		if (visible) {
			r.andExpect(status().isOk());
		} else {
			r.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEED_POST_NOT_FOUND"));
		}
	}

	@Test
	void tf1_gorunurluk() throws Exception {
		long admin = data.admin();
		long kuzey = data.branch("Kuzey");
		long guney = data.branch("Guney");
		long season = data.season("2026-2027", "ACTIVE");
		long author = data.user("Yazar");
		long uye = data.user("Uye");
		long disari = data.user("Disari");
		long guneyli = data.user("Guneyli");
		data.member(season, admin, kuzey, null);
		data.member(season, author, kuzey, null);
		data.member(season, uye, kuzey, null);
		data.member(season, disari, kuzey, null);
		data.member(season, guneyli, guney, null);
		long g = data.group("Zumre", author, author, uye);

		long gp = newPost(season, author, "GROUP", g, false);
		assertVisible(uye, gp, true);
		assertVisible(disari, gp, false);

		jdbc.sql("DELETE FROM staff_group_member WHERE group_id = :g AND user_id = :u").param("g", g).param("u", uye).update();
		assertVisible(uye, gp, false);

		long bp = newPost(season, author, "BRANCH", guney, false);
		assertVisible(guneyli, bp, true);
		assertVisible(uye, bp, false);
		assertVisible(admin, bp, false);

		long ap = newPost(season, author, "ALL", null, false);
		assertVisible(uye, ap, true);
		assertVisible(guneyli, ap, true);
	}

	@Test
	void tf2_yetki() throws Exception {
		long kuzey = data.branch("Kuzey");
		long season = data.season("2026-2027", "ACTIVE");
		long me = data.user("Personel");
		long other = data.user("Diger");
		data.member(season, me, kuzey, null);
		data.member(season, other, kuzey, null);
		long g = data.group("Baska", other, other);

		mvc.perform(post("/api/feed/posts").with(loginAs(me)).contentType(APPLICATION_JSON)
				.content(json(Map.of("targets", List.of(Map.of("type", "ALL")), "content", "x", "fileIds", List.of(), "important", false))))
				.andExpect(status().is4xxClientError()).andExpect(jsonPath("$.code").value("TARGET_TYPE_NOT_ALLOWED"));

		mvc.perform(post("/api/feed/posts").with(loginAs(me)).contentType(APPLICATION_JSON)
				.content(json(Map.of("targets", List.of(Map.of("type", "USER", "id", other)), "content", "x", "fileIds", List.of(), "important", true))))
				.andExpect(status().is4xxClientError()).andExpect(jsonPath("$.code").value("FEED_IMPORTANT_FORBIDDEN"));

		mvc.perform(post("/api/feed/posts").with(loginAs(me)).contentType(APPLICATION_JSON)
				.content(json(Map.of("targets", List.of(Map.of("type", "GROUP", "id", g)), "content", "x", "fileIds", List.of(), "important", false))))
				.andExpect(status().is4xxClientError()).andExpect(jsonPath("$.code").value("FEED_GROUP_NOT_MEMBER"));
	}

	@Test
	void tf3_onemliDuyuruVeArsiv() throws Exception {
		long admin = data.admin();
		long kuzey = data.branch("Kuzey");
		long season = data.season("2026-2027", "ACTIVE");
		long old = data.season("2025-2026", "CLOSED");
		long a = data.user("Ali");
		long b = data.user("Burak");
		long c = data.user("Can");
		for (long u : new long[] { admin, a, b, c }) {
			data.member(season, u, kuzey, null);
			data.member(old, u, kuzey, null);
		}
		long p = newPost(season, admin, "ALL", null, true);
		jdbc.sql("INSERT INTO feed_read (post_id, user_id) VALUES (:p, :u)").param("p", p).param("u", a).update();

		mvc.perform(get("/api/feed/posts/" + p + "/reads").with(loginAs(admin))).andExpect(status().isOk())
				.andExpect(jsonPath("$.read.length()").value(1)).andExpect(jsonPath("$.unread.length()").value(2));

		mvc.perform(post("/api/feed/posts/" + p + "/remind").with(loginAs(admin))).andExpect(status().isOk())
				.andExpect(jsonPath("$.notified").value(2));
		mvc.perform(post("/api/feed/posts/" + p + "/remind").with(loginAs(admin))).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.code").value("FEED_REMIND_TOO_SOON"));

		long oldPost = newPost(old, admin, "ALL", null, false);
		mvc.perform(post("/api/feed/posts/" + oldPost + "/like").with(loginAs(a))).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.code").value("SEASON_READ_ONLY"));
	}

}
