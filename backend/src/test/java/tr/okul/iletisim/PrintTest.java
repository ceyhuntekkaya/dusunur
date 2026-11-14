package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tr.okul.iletisim.print.PrintJobs;

class PrintTest extends IntegrationTest {

	@Autowired
	PrintJobs jobs;

	private long create(long user) throws Exception {
		String body = json(Map.of("title", "Sinav kagidi", "originalOnPaper", true, "pageCount", 2, "copies", 3,
				"color", "BW", "sides", "SINGLE", "paperSize", "A4", "secret", false,
				"dueAt", OffsetDateTime.now().plusDays(3).toString()));
		String res = mvc.perform(post("/api/print/requests").with(loginAs(user)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(res).get("id").asLong();
	}

	private long insert(long season, long requester, long branch, String status, Long approver) {
		return jdbc.sql("""
				INSERT INTO print_request (season_id, requester_id, branch_id, title, original_on_paper, page_count, copies,
				  color, sides, paper_size, due_at, total_pages, status, approver_id)
				VALUES (:s, :r, :b, 'Talep', true, 1, 1, 'BW', 'SINGLE', 'A4', now() + interval '3 days', 1, :st, :a) RETURNING id
				""").param("s", season).param("r", requester).param("b", branch).param("st", status)
				.param("a", approver, java.sql.Types.BIGINT).query(Long.class).single();
	}

	private void makeOperator(long season, long user) {
		jdbc.sql("UPDATE season_membership SET is_print_operator = true WHERE season_id = :s AND user_id = :u")
				.param("s", season).param("u", user).update();
	}

	private Map<String, Object> row(long id) {
		return jdbc.sql("SELECT * FROM print_request WHERE id = :id").param("id", id).query().singleRow();
	}

	private void delegate(long approver, long delegate) {
		LocalDate today = LocalDate.now(ZoneId.of("Europe/Istanbul"));
		jdbc.sql("INSERT INTO print_delegation (approver_id, delegate_id, start_date, end_date) VALUES (:a, :d, :s, :e)")
				.param("a", approver).param("d", delegate).param("s", today.minusDays(1)).param("e", today.plusDays(2))
				.update();
	}

	@Test
	void tp1_onaylayiciBelirleme() throws Exception {
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long approver = data.user("Onaylayici");
		long vekil = data.user("Vekil");
		long a = data.user("Ali");
		long b = data.user("Burak");
		long c = data.user("Can");
		data.member(season, approver, branch, null);
		data.member(season, vekil, branch, null);
		data.member(season, a, branch, approver);
		data.member(season, b, branch, null);

		long ra = create(a);
		assertThat(row(ra)).containsEntry("status", "PENDING_APPROVAL").containsEntry("approver_id", approver);

		long rb = create(b);
		assertThat(row(rb)).containsEntry("status", "APPROVED").containsEntry("approver_id", null);

		delegate(approver, vekil);
		long rc = create(a);
		assertThat(row(rc)).containsEntry("approver_id", vekil).containsEntry("original_approver_id", approver);

		data.member(season, c, branch, approver);
		jdbc.sql("DELETE FROM print_delegation").update();
		delegate(approver, c);
		long rd = create(c);
		assertThat(row(rd)).containsEntry("approver_id", approver).containsEntry("original_approver_id", null);
	}

	@Test
	void tp2_durumMakinesi() throws Exception {
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long approver = data.user("Onaylayici");
		long operator = data.user("Operator");
		long a = data.user("Ali");
		long other = data.user("Diger");
		data.member(season, approver, branch, null);
		data.member(season, operator, branch, null);
		data.member(season, a, branch, approver);
		data.member(season, other, branch, null);
		makeOperator(season, operator);

		long id = create(a);
		String base = "/api/print/requests/" + id;
		mvc.perform(post(base + "/approve").with(loginAs(approver))).andExpect(status().isOk());
		mvc.perform(post(base + "/deliver").with(loginAs(operator))).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.code").value("PRINT_INVALID_STATUS"));
		mvc.perform(post(base + "/start").with(loginAs(operator))).andExpect(status().isOk());
		mvc.perform(post(base + "/cancel").with(loginAs(a))).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.code").value("PRINT_CANCEL_NOT_ALLOWED"));
		mvc.perform(post(base + "/ready").with(loginAs(operator))).andExpect(status().isOk());
		mvc.perform(post(base + "/deliver").with(loginAs(operator))).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"));
		Long history = jdbc.sql("SELECT count(*) FROM print_request_history WHERE request_id = :r").param("r", id)
				.query(Long.class).single();
		assertThat(history).isEqualTo(5);

		long id2 = create(a);
		mvc.perform(post("/api/print/requests/" + id2 + "/approve").with(loginAs(other)))
				.andExpect(status().is4xxClientError()).andExpect(jsonPath("$.code").value("PRINT_APPROVE_FORBIDDEN"));
		mvc.perform(post("/api/print/requests/" + id2 + "/reject").with(loginAs(approver)).contentType(APPLICATION_JSON)
				.content(json(Map.of("reason", "  "))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void tp3_gorunurluk() throws Exception {
		long kuzey = data.branch("Kuzey");
		long guney = data.branch("Guney");
		long season = data.season("2026-2027", "ACTIVE");
		long approver = data.user("Onaylayici");
		long a = data.user("Ali");
		long kuzeyOp = data.user("KuzeyOp");
		long guneyOp = data.user("GuneyOp");
		data.member(season, approver, kuzey, null);
		data.member(season, a, kuzey, approver);
		data.member(season, kuzeyOp, kuzey, null);
		data.member(season, guneyOp, guney, null);
		makeOperator(season, kuzeyOp);
		makeOperator(season, guneyOp);

		long approved = insert(season, a, kuzey, "APPROVED", null);
		mvc.perform(get("/api/print/requests/" + approved).with(loginAs(kuzeyOp))).andExpect(status().isOk());
		mvc.perform(get("/api/print/requests/" + approved).with(loginAs(guneyOp))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRINT_REQUEST_NOT_FOUND"));

		long pending = insert(season, a, kuzey, "PENDING_APPROVAL", approver);
		mvc.perform(get("/api/print/requests/" + pending).with(loginAs(kuzeyOp))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRINT_REQUEST_NOT_FOUND"));
	}

	@Test
	void tp4_hooklar() throws Exception {
		long admin = data.admin();
		long branch = data.branch("Merkez");
		long old = data.season("2025-2026", "ACTIVE");
		long draft = data.season("2026-2027", "DRAFT");
		long approver = data.user("Onaylayici");
		long successor = data.user("Devralan");
		long a = data.user("Ali");
		for (long s : new long[] { old, draft }) {
			data.member(s, approver, branch, null);
			data.member(s, successor, branch, null);
			data.member(s, a, branch, approver);
		}
		long open = insert(old, a, branch, "PENDING_APPROVAL", approver);
		long approved = insert(old, a, branch, "APPROVED", null);

		mvc.perform(post("/api/admin/seasons/" + draft + "/activate").with(loginAs(admin))).andExpect(status().isOk());
		assertThat(row(open)).containsEntry("status", "CANCELLED");
		assertThat(row(approved)).containsEntry("status", "CANCELLED");

		long pending = insert(draft, a, branch, "PENDING_APPROVAL", approver);
		mvc.perform(post("/api/admin/users/" + approver + "/deactivate").with(loginAs(admin))
				.contentType(APPLICATION_JSON).content(json(Map.of("successorUserId", successor))))
				.andExpect(status().isNoContent());
		assertThat(row(pending)).containsEntry("status", "PENDING_APPROVAL").containsEntry("approver_id", successor);
	}

	@Test
	void tp5_temizleme() {
		long branch = data.branch("Merkez");
		long season = data.season("2026-2027", "ACTIVE");
		long a = data.user("Ali");
		data.member(season, a, branch, null);
		long oldReq = insert(season, a, branch, "DELIVERED", null);
		long newReq = insert(season, a, branch, "DELIVERED", null);
		UUID oldFile = attachFile(oldReq, a, OffsetDateTime.now().minusYears(5));
		UUID newFile = attachFile(newReq, a, OffsetDateTime.now());

		jobs.purgeFiles();

		assertThat(row(oldReq).get("file_purged_at")).isNotNull();
		assertThat(row(newReq).get("file_purged_at")).isNull();
		assertThat(deletedAt(oldFile)).isNotNull();
		assertThat(deletedAt(newFile)).isNull();
	}

	private UUID attachFile(long requestId, long user, OffsetDateTime deliveredAt) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO stored_file (id, original_name, content_type, size_bytes, storage_path, purpose, uploaded_by, attached)
				VALUES (:id, 'a.pdf', 'application/pdf', 1, :p, 'PRINT', :u, true)
				""").param("id", id).param("p", "test/" + id).param("u", user).update();
		jdbc.sql("UPDATE print_request SET file_id = :f, delivered_at = :d WHERE id = :r").param("f", id)
				.param("d", deliveredAt).param("r", requestId).update();
		return id;
	}

	private Object deletedAt(UUID file) {
		return jdbc.sql("SELECT deleted_at FROM stored_file WHERE id = :id").param("id", file).query().singleRow()
				.get("deleted_at");
	}

}
