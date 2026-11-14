package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import tools.jackson.databind.JsonNode;
import tr.okul.iletisim.core.spi.GroupArchivedEvent;
import tr.okul.iletisim.core.spi.GroupCreatedEvent;
import tr.okul.iletisim.core.spi.GroupMembersRemovedEvent;

class ChatTest extends IntegrationTest {

	@Autowired
	ApplicationEventPublisher events;

	private long season;
	private long branch;

	private long staff(String name) {
		if (season == 0) {
			branch = data.branch("Merkez");
			season = data.season("2026-2027", "ACTIVE");
		}
		long id = data.user(name);
		data.member(season, id, branch, null);
		return id;
	}

	private JsonNode read(String res) throws Exception {
		return mapper.readTree(res);
	}

	private long direct(long me, long other) throws Exception {
		String res = mvc.perform(post("/api/chat/direct").with(loginAs(me)).contentType(APPLICATION_JSON)
				.content(json(Map.of("userId", other)))).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		return read(res).get("id").asLong();
	}

	private String sendBody(String content, UUID clientId) {
		return json(Map.of("clientId", clientId.toString(), "content", content, "fileIds", List.of()));
	}

	private JsonNode send(long me, long conv, String content) throws Exception {
		String res = mvc.perform(post("/api/chat/conversations/" + conv + "/messages").with(loginAs(me))
				.contentType(APPLICATION_JSON).content(sendBody(content, UUID.randomUUID())))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return read(res);
	}

	private JsonNode detail(long me, long conv) throws Exception {
		return read(mvc.perform(get("/api/chat/conversations/" + conv).with(loginAs(me))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
	}

	private int unread(long me, long conv) throws Exception {
		JsonNode rows = read(mvc.perform(get("/api/chat/conversations").with(loginAs(me))).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString());
		for (JsonNode r : rows) {
			if (r.get("id").asLong() == conv) {
				return r.get("unreadCount").asInt();
			}
		}
		throw new AssertionError("Sohbet listede yok");
	}

	private long groupChat(long groupId) {
		return jdbc.sql("SELECT id FROM chat_conversation WHERE staff_group_id = :g").param("g", groupId)
				.query(Long.class).single();
	}

	@Test
	void tm1_birebirSohbet() throws Exception {
		long a = staff("Ali");
		long b = staff("Burak");

		long c1 = direct(a, b);
		long c2 = direct(a, b);
		assertThat(c2).isEqualTo(c1);
		assertThat(direct(b, a)).isEqualTo(c1);

		long mid = send(a, c1, "Merhaba").get("id").asLong();
		assertThat(unread(b, c1)).isEqualTo(1);

		mvc.perform(post("/api/chat/conversations/" + c1 + "/read").with(loginAs(b)).contentType(APPLICATION_JSON)
				.content(json(Map.of("lastMessageId", mid)))).andExpect(status().isNoContent());
		assertThat(unread(b, c1)).isZero();
		assertThat(detail(a, c1).get("otherLastReadMessageId").asLong()).isEqualTo(mid);
	}

	@Test
	void tm2_erisim() throws Exception {
		long a = staff("Ali");
		long b = staff("Burak");
		long c = staff("Can");
		long conv = direct(a, b);
		send(a, conv, "Gizli");

		mvc.perform(get("/api/chat/conversations/" + conv + "/messages").with(loginAs(c)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CHAT_CONVERSATION_NOT_FOUND"));

		long g = data.group("Zumre", a, a, b, c);
		events.publishEvent(new GroupCreatedEvent(g, "Zumre", List.of(a, b, c)));
		long gc = groupChat(g);
		mvc.perform(get("/api/chat/conversations/" + gc + "/messages").with(loginAs(c))).andExpect(status().isOk());

		events.publishEvent(new GroupMembersRemovedEvent(g, List.of(c)));
		mvc.perform(get("/api/chat/conversations/" + gc + "/messages").with(loginAs(c)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CHAT_CONVERSATION_NOT_FOUND"));
	}

	@Test
	void tm3_grupOlaylari() throws Exception {
		long a = staff("Ali");
		long b = staff("Burak");
		long c = staff("Can");
		long g = data.group("Zumre", a, a, b, c);
		events.publishEvent(new GroupCreatedEvent(g, "Zumre", List.of(a, b, c)));

		long gc = groupChat(g);
		assertThat(jdbc.sql("SELECT type FROM chat_conversation WHERE id = :i").param("i", gc).query(String.class)
				.single()).isEqualTo("GROUP");
		List<Long> members = jdbc.sql("SELECT user_id FROM chat_member WHERE conversation_id = :i ORDER BY user_id")
				.param("i", gc).query(Long.class).list();
		assertThat(members).containsExactlyInAnyOrder(a, b, c);

		events.publishEvent(new GroupArchivedEvent(g, true));
		mvc.perform(post("/api/chat/conversations/" + gc + "/messages").with(loginAs(a)).contentType(APPLICATION_JSON)
				.content(sendBody("Selam", UUID.randomUUID()))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CHAT_READ_ONLY"));
	}

	@Test
	void tm4_mesaj() throws Exception {
		long a = staff("Ali");
		long b = staff("Burak");
		long conv = direct(a, b);

		long mid = send(a, conv, "Silinecek").get("id").asLong();
		mvc.perform(delete("/api/chat/messages/" + mid).with(loginAs(b))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CHAT_DELETE_FORBIDDEN"));

		mvc.perform(delete("/api/chat/messages/" + mid).with(loginAs(a))).andExpect(status().isNoContent());
		String page = mvc.perform(get("/api/chat/conversations/" + conv + "/messages").with(loginAs(a)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode msg = read(page).get("items").get(0);
		assertThat(msg.get("deleted").asBoolean()).isTrue();
		assertThat(msg.get("content") == null || msg.get("content").isNull()).isTrue();

		UUID cid = UUID.randomUUID();
		String url = "/api/chat/conversations/" + conv + "/messages";
		String r1 = mvc.perform(post(url).with(loginAs(a)).contentType(APPLICATION_JSON).content(sendBody("Bir", cid)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String r2 = mvc.perform(post(url).with(loginAs(a)).contentType(APPLICATION_JSON).content(sendBody("Bir", cid)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(read(r2).get("id").asLong()).isEqualTo(read(r1).get("id").asLong());
		assertThat(jdbc.sql("SELECT count(*) FROM chat_message WHERE client_id = :c").param("c", cid)
				.query(Long.class).single()).isEqualTo(1L);
	}

	@Test
	void tm5_serbestGrupSahiplik() throws Exception {
		long a = staff("Ali");
		long b = staff("Burak");
		long c = staff("Can");
		String res = mvc.perform(post("/api/chat/groups").with(loginAs(a)).contentType(APPLICATION_JSON)
				.content(json(Map.of("title", "Piknik", "memberIds", List.of(b, c))))).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long conv = read(res).get("id").asLong();
		jdbc.sql("UPDATE chat_member SET joined_at = now() - interval '3 days' WHERE conversation_id = :c AND user_id = :u")
				.param("c", conv).param("u", c).update();
		jdbc.sql("UPDATE chat_member SET joined_at = now() - interval '1 day' WHERE conversation_id = :c AND user_id = :u")
				.param("c", conv).param("u", b).update();

		mvc.perform(post("/api/chat/conversations/" + conv + "/leave").with(loginAs(a)))
				.andExpect(status().isNoContent());

		String owner = jdbc.sql("SELECT user_id FROM chat_member WHERE conversation_id = :c AND role = 'OWNER'")
				.param("c", conv).query(String.class).single();
		assertThat(owner).isEqualTo(String.valueOf(c));
		mvc.perform(get("/api/chat/conversations/" + conv).with(loginAs(a))).andExpect(status().isNotFound());
	}

}
