package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import tr.okul.iletisim.dms.DmsJobs;

class DmsTest extends IntegrationTest {

	@Autowired
	DmsJobs jobs;

	private long space(String name, long owner) {
		return jdbc.sql("INSERT INTO dms_space (name, name_key, owner_id) VALUES (:n, :k, :o) RETURNING id")
				.param("n", name).param("k", name.toLowerCase()).param("o", owner).query(Long.class).single();
	}

	private void grant(long space, String type, Long subject, int level, long by) {
		jdbc.sql("""
				INSERT INTO dms_space_grant (space_id, subject_type, subject_id, level, created_by)
				VALUES (:s, :t, :i, :l, :c)
				""").param("s", space).param("t", type).param("i", subject, java.sql.Types.BIGINT).param("l", level)
				.param("c", by).update();
	}

	private UUID stored(long user, String name) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO stored_file (id, original_name, content_type, size_bytes, storage_path, purpose, uploaded_by)
				VALUES (:id, :n, 'application/pdf', 1, :p, 'DMS', :u)
				""").param("id", id).param("n", name).param("p", "test/" + id).param("u", user).update();
		return id;
	}

	private ResultActions upload(long user, long space, Long folder, UUID file, String onConflict) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("folderId", folder);
		body.put("fileId", file.toString());
		body.put("onConflict", onConflict);
		return mvc.perform(post("/api/dms/spaces/" + space + "/files").with(loginAs(user)).contentType(APPLICATION_JSON)
				.content(json(body)));
	}

	private long folder(long user, long space, Long parent, String name) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("parentId", parent);
		body.put("name", name);
		String res = mvc.perform(post("/api/dms/spaces/" + space + "/folders").with(loginAs(user))
				.contentType(APPLICATION_JSON).content(json(body))).andExpect(status().isCreated()).andReturn()
				.getResponse().getContentAsString();
		return mapper.readTree(res).get("id").asLong();
	}

	private long id(ResultActions r) throws Exception {
		return mapper.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
	}

	private Map<String, Object> file(long id) {
		return jdbc.sql("SELECT * FROM dms_file WHERE id = :id").param("id", id).query().singleRow();
	}

	private Object storedDeletedAt(Object fileId) {
		return jdbc.sql("SELECT deleted_at FROM stored_file WHERE id = :id").param("id", fileId).query().singleRow()
				.get("deleted_at");
	}

	@Test
	void td1_yetki() throws Exception {
		long b1 = data.branch("Kuzey");
		long b2 = data.branch("Guney");
		long season = data.season("2026-2027", "ACTIVE");
		long owner = data.user("Sahip");
		long u = data.user("Ali");
		long outsider = data.user("Disarida");
		data.member(season, owner, b1, null);
		data.member(season, u, b1, null);
		data.member(season, outsider, b2, null);
		long g = data.group("Zumre", owner, u);
		long sp = space("Alan", owner);

		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(outsider))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DMS_SPACE_NOT_FOUND"));

		grant(sp, "USER", u, 1, owner);
		grant(sp, "GROUP", g, 2, owner);
		grant(sp, "BRANCH", b1, 1, owner);
		grant(sp, "ALL", null, 1, owner);
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(u))).andExpect(status().isOk())
				.andExpect(jsonPath("$.myLevel").value("EDIT"));
		jdbc.sql("UPDATE dms_space_grant SET level = 3 WHERE subject_type = 'BRANCH'").update();
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(u))).andExpect(jsonPath("$.myLevel").value("MANAGE"));
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(owner))).andExpect(jsonPath("$.myLevel").value("MANAGE"));
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(outsider))).andExpect(status().isOk())
				.andExpect(jsonPath("$.myLevel").value("VIEW"));

		jdbc.sql("DELETE FROM dms_space_grant WHERE subject_type = 'BRANCH'").update();
		long admin = data.admin();
		mvc.perform(delete("/api/groups/" + g + "/members/" + u).with(loginAs(admin))).andExpect(status().isNoContent());
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(u))).andExpect(jsonPath("$.myLevel").value("VIEW"));

		jdbc.sql("DELETE FROM dms_space_grant WHERE subject_type = 'ALL'").update();
		jdbc.sql("UPDATE dms_space_grant SET level = 1 WHERE subject_type = 'USER'").update();
		upload(u, sp, null, stored(u, "a.pdf"), "ASK").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("DMS_EDIT_FORBIDDEN"));
	}

	@Test
	void td2_adCakismasi() throws Exception {
		long b = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long owner = data.user("Sahip");
		data.member(season, owner, b, null);
		long sp = space("Alan", owner);

		UUID first = stored(owner, "Rapor.pdf");
		long fileId = id(upload(owner, sp, null, first, "ASK").andExpect(status().isCreated()));

		UUID second = stored(owner, "Rapor.pdf");
		upload(owner, sp, null, second, "ASK").andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DMS_NAME_CONFLICT"))
				.andExpect(jsonPath("$.details[0]").value("Rapor.pdf"));

		upload(owner, sp, null, second, "OVERWRITE").andExpect(status().isOk());
		assertThat(file(fileId).get("file_id")).isEqualTo(second);
		assertThat(storedDeletedAt(first)).isNotNull();
		assertThat(storedDeletedAt(second)).isNull();

		UUID third = stored(owner, "Rapor.pdf");
		upload(owner, sp, null, third, "RENAME").andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Rapor (2).pdf"));
	}

	@Test
	void td3_copKutusu() throws Exception {
		long b = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long owner = data.user("Sahip");
		data.member(season, owner, b, null);
		long sp = space("Alan", owner);

		long f = folder(owner, sp, null, "Klasor");
		long sub = folder(owner, sp, f, "Alt");
		UUID stored = stored(owner, "ic.pdf");
		long inner = id(upload(owner, sp, f, stored, "ASK").andExpect(status().isCreated()));

		mvc.perform(delete("/api/dms/folders/" + f).with(loginAs(owner))).andExpect(status().isNoContent());
		mvc.perform(get("/api/dms/spaces/" + sp + "/items").with(loginAs(owner))).andExpect(status().isOk())
				.andExpect(jsonPath("$.folders.length()").value(0)).andExpect(jsonPath("$.files.length()").value(0));
		mvc.perform(get("/api/dms/spaces/" + sp + "/trash").with(loginAs(owner))).andExpect(jsonPath("$.length()").value(1));

		mvc.perform(post("/api/dms/trash/folder/" + f + "/restore").with(loginAs(owner))).andExpect(status().isOk());
		mvc.perform(get("/api/dms/spaces/" + sp + "/items").param("folderId", String.valueOf(f)).with(loginAs(owner)))
				.andExpect(jsonPath("$.folders.length()").value(1)).andExpect(jsonPath("$.files.length()").value(1));
		assertThat(file(inner).get("deleted_at")).isNull();
		assertThat(jdbc.sql("SELECT deleted_at FROM dms_folder WHERE id = :id").param("id", sub).query().singleRow()
				.get("deleted_at")).isNull();

		mvc.perform(delete("/api/dms/folders/" + f).with(loginAs(owner))).andExpect(status().isNoContent());
		UUID loose = stored(owner, "tek.pdf");
		long looseId = id(upload(owner, sp, null, loose, "ASK"));
		mvc.perform(delete("/api/dms/files/" + looseId).with(loginAs(owner))).andExpect(status().isNoContent());
		UUID fresh = stored(owner, "yeni.pdf");
		long freshId = id(upload(owner, sp, null, fresh, "ASK"));
		mvc.perform(delete("/api/dms/files/" + freshId).with(loginAs(owner))).andExpect(status().isNoContent());

		jdbc.sql("UPDATE dms_folder SET deleted_at = now() - interval '40 days' WHERE id = :id").param("id", f).update();
		jdbc.sql("UPDATE dms_file SET deleted_at = now() - interval '40 days' WHERE id = :id").param("id", looseId).update();
		jobs.purgeTrash();

		assertThat(jdbc.sql("SELECT count(*) FROM dms_folder WHERE space_id = :s").param("s", sp).query(Long.class).single())
				.isZero();
		assertThat(jdbc.sql("SELECT count(*) FROM dms_file WHERE id IN (:a, :b)").param("a", inner).param("b", looseId)
				.query(Long.class).single()).isZero();
		assertThat(storedDeletedAt(stored)).isNotNull();
		assertThat(storedDeletedAt(loose)).isNotNull();
		assertThat(file(freshId)).isNotNull();
		assertThat(storedDeletedAt(fresh)).isNull();
	}

	@Test
	void td4_hookVeYil() throws Exception {
		long admin = data.admin();
		long b = data.branch("Merkez");
		long old = data.season("2025-2026", "ACTIVE");
		long draft = data.season("2026-2027", "DRAFT");
		long owner = data.user("Sahip");
		long successor = data.user("Devralan");
		long u = data.user("Ali");
		long all = data.user("Herkes");
		for (long s : new long[] { old, draft }) {
			for (long m : new long[] { owner, successor, u, all }) {
				data.member(s, m, b, null);
			}
		}
		long sp = space("Alan", owner);
		grant(sp, "USER", u, 2, owner);
		grant(sp, "ALL", null, 1, owner);
		grant(sp, "USER", owner, 2, successor);

		mvc.perform(post("/api/admin/seasons/" + draft + "/activate").with(loginAs(admin))).andExpect(status().isOk());
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(u))).andExpect(status().isOk())
				.andExpect(jsonPath("$.myLevel").value("EDIT"));
		mvc.perform(get("/api/dms/spaces/" + sp).with(loginAs(all))).andExpect(status().isOk())
				.andExpect(jsonPath("$.myLevel").value("VIEW"));

		mvc.perform(post("/api/admin/users/" + owner + "/deactivate").with(loginAs(admin)).contentType(APPLICATION_JSON)
				.content(json(Map.of("successorUserId", successor)))).andExpect(status().isNoContent());
		assertThat(jdbc.sql("SELECT owner_id FROM dms_space WHERE id = :id").param("id", sp).query(Long.class).single())
				.isEqualTo(successor);
		assertThat(jdbc.sql("SELECT count(*) FROM dms_space_grant WHERE subject_type = 'USER' AND subject_id = :u")
				.param("u", owner).query(Long.class).single()).isZero();
		assertThat(jdbc.sql("SELECT count(*) FROM notification WHERE user_id = :u AND type = 'DMS_SPACE_OWNER_CHANGED'")
				.param("u", successor).query(Long.class).single()).isEqualTo(1);
	}

	@Test
	void td5_tasima() throws Exception {
		long b = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long owner = data.user("Sahip");
		data.member(season, owner, b, null);
		long sp = space("Alan", owner);
		long f = folder(owner, sp, null, "Ust");
		long sub = folder(owner, sp, f, "Alt");

		for (long target : new long[] { sub, f }) {
			mvc.perform(post("/api/dms/folders/" + f + "/move").with(loginAs(owner)).contentType(APPLICATION_JSON)
					.content(json(Map.of("targetFolderId", target)))).andExpect(status().is4xxClientError())
					.andExpect(jsonPath("$.code").value("DMS_MOVE_INVALID"));
		}
	}

}
