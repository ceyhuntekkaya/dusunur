package tr.okul.iletisim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tr.okul.iletisim.calendar.CalendarBootstrap;
import tr.okul.iletisim.core.spi.GroupArchivedEvent;

class CalendarTest extends IntegrationTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

	@Autowired
	CalendarBootstrap bootstrap;

	@Autowired
	ApplicationEventPublisher events;

	private long season;
	private long branch;

	private long staff(String name, boolean manager) {
		if (season == 0) {
			branch = data.branch("Merkez");
			season = data.season("2026-2027", "ACTIVE");
		}
		long id = data.user(name);
		data.member(season, id, branch, null);
		if (manager) {
			jdbc.sql("UPDATE season_membership SET is_manager = true WHERE user_id = :u").param("u", id).update();
		}
		return id;
	}

	private long staff(String name) {
		return staff(name, false);
	}

	private long personal(long owner, String name) throws Exception {
		String res = mvc.perform(post("/api/calendar/calendars").with(loginAs(owner)).contentType(APPLICATION_JSON)
				.content(json(Map.of("name", name)))).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return mapper.readTree(res).get("id").asLong();
	}

	private long resource(long admin, String name) throws Exception {
		String res = mvc.perform(post("/api/calendar/resources").with(loginAs(admin)).contentType(APPLICATION_JSON)
				.content(json(Map.of("name", name)))).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return mapper.readTree(res).get("id").asLong();
	}

	private long calendarOf(String type) {
		bootstrap.ensureSystem();
		return jdbc.sql("SELECT id FROM cal_calendar WHERE type = :t").param("t", type).query(Long.class).single();
	}

	private long groupCalendar(long groupId) {
		bootstrap.ensureSystem();
		return jdbc.sql("SELECT id FROM cal_calendar WHERE staff_group_id = :g").param("g", groupId)
				.query(Long.class).single();
	}

	private void share(long owner, long cal, String type, long subject, String level) throws Exception {
		mvc.perform(post("/api/calendar/calendars/" + cal + "/shares").with(loginAs(owner))
				.contentType(APPLICATION_JSON)
				.content(json(Map.of("subject", Map.of("type", type, "id", subject), "level", level))))
				.andExpect(status().is2xxSuccessful());
	}

	private Map<String, Object> timed(long cal, String title, OffsetDateTime start, OffsetDateTime end) {
		Map<String, Object> body = new HashMap<>();
		body.put("calendarId", cal);
		body.put("title", title);
		body.put("allDay", false);
		body.put("startAt", start.toString());
		body.put("endAt", end.toString());
		return body;
	}

	private OffsetDateTime at(int hour) {
		return LocalDate.now(ZONE).plusDays(10).atTime(hour, 0).atZone(ZONE).toOffsetDateTime();
	}

	private ResultActions addEntry(long user, Map<String, Object> body) throws Exception {
		return mvc.perform(post("/api/calendar/entries").with(loginAs(user)).contentType(APPLICATION_JSON)
				.content(json(body)));
	}

	private ResultActions editEntry(long user, long id, Map<String, Object> body) throws Exception {
		return mvc.perform(put("/api/calendar/entries/" + id).with(loginAs(user)).contentType(APPLICATION_JSON)
				.content(json(body)));
	}

	private long entryId(ResultActions r) throws Exception {
		return mapper.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
	}

	@Test
	void tk1_kisiselTakvim() throws Exception {
		long owner = staff("Ali");
		long viewer = staff("Burak");
		long editor = staff("Can");
		long member = staff("Deniz");
		long cal = personal(owner, "Planlarim");

		mvc.perform(get("/api/calendar/entries?from=" + at(0).toInstant() + "&to=" + at(23).toInstant()
				+ "&calendarIds=" + cal).with(loginAs(viewer))).andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
		mvc.perform(post("/api/calendar/entries").with(loginAs(viewer)).contentType(APPLICATION_JSON)
				.content(json(timed(cal, "Gizli", at(9), at(10))))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CAL_CALENDAR_NOT_FOUND"));

		share(owner, cal, "USER", viewer, "VIEW");
		addEntry(viewer, timed(cal, "Deneme", at(9), at(10))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CAL_CREATE_FORBIDDEN"));

		share(owner, cal, "USER", editor, "EDIT");
		long entry = entryId(addEntry(owner, timed(cal, "Sahibin kaydi", at(9), at(10))).andExpect(status().isCreated()));
		addEntry(editor, timed(cal, "Editor kaydi", at(11), at(12))).andExpect(status().isCreated());
		editEntry(editor, entry, timed(cal, "Degisti", at(9), at(10))).andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Degisti"));

		long g = data.group("Zumre", owner, owner, member);
		share(owner, cal, "GROUP", g, "EDIT");
		addEntry(member, timed(cal, "Grup uyesi", at(13), at(14))).andExpect(status().isCreated());
	}

	@Test
	void tk2_grupTakvimi() throws Exception {
		long admin = staff("Yonetici", true);
		long a = staff("Ali");
		long b = staff("Burak");
		long g = data.group("Zumre", admin, a, b);
		long cal = groupCalendar(g);

		long entry = entryId(addEntry(a, timed(cal, "Toplanti", at(9), at(10))).andExpect(status().isCreated()));
		editEntry(b, entry, timed(cal, "Baska", at(9), at(10))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CAL_EDIT_FORBIDDEN"));
		editEntry(admin, entry, timed(cal, "Yonetici", at(9), at(10))).andExpect(status().isOk());

		jdbc.sql("UPDATE staff_group SET archived = true WHERE id = :g").param("g", g).update();
		events.publishEvent(new GroupArchivedEvent(g, true));
		addEntry(a, timed(cal, "Yeni", at(15), at(16))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CAL_READ_ONLY"));
	}

	@Test
	void tk3_salon() throws Exception {
		long admin = staff("Yonetici", true);
		long a = staff("Ali");
		long cal = resource(admin, "Konferans Salonu");

		long first = entryId(addEntry(a, timed(cal, "Ilk", at(10), at(11))).andExpect(status().isCreated()));
		addEntry(a, timed(cal, "Cakisan", OffsetDateTime.parse(at(10).plusMinutes(30).toString()), at(12)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAL_RESOURCE_CONFLICT"));
		addEntry(a, timed(cal, "Uc uca", at(11), at(12))).andExpect(status().isCreated());
		editEntry(a, first, timed(cal, "Ilk", at(10), at(11).minusMinutes(15))).andExpect(status().isOk());
	}

	@Test
	void tk4_kurumTakvimi() throws Exception {
		long admin = staff("Yonetici", true);
		long a = staff("Ali");
		long cal = calendarOf("INSTITUTION");

		addEntry(a, timed(cal, "Tatil", at(9), at(10))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CAL_CREATE_FORBIDDEN"));
		addEntry(admin, timed(cal, "Tatil", at(9), at(10))).andExpect(status().isCreated());
	}

	@Test
	void tk5_tumGun() throws Exception {
		long a = staff("Ali");
		long cal = personal(a, "Gunler");
		Map<String, Object> body = new HashMap<>();
		body.put("calendarId", cal);
		body.put("title", "Gezi");
		body.put("allDay", true);
		body.put("startDate", "2026-11-14");
		body.put("endDate", "2026-11-16");

		ResultActions res = addEntry(a, body).andExpect(status().isCreated())
				.andExpect(jsonPath("$.startDate").value("2026-11-14"))
				.andExpect(jsonPath("$.endDate").value("2026-11-16"));
		long id = entryId(res);

		Map<String, Object> row = jdbc.sql("SELECT start_at, end_at FROM cal_entry WHERE id = :i").param("i", id)
				.query().singleRow();
		OffsetDateTime start = ((java.sql.Timestamp) toTs(row.get("start_at"))).toInstant().atZone(ZONE)
				.toOffsetDateTime();
		OffsetDateTime end = ((java.sql.Timestamp) toTs(row.get("end_at"))).toInstant().atZone(ZONE)
				.toOffsetDateTime();
		assertThat(start).isEqualTo(OffsetDateTime.parse("2026-11-14T00:00+03:00"));
		assertThat(end).isEqualTo(OffsetDateTime.parse("2026-11-17T00:00+03:00"));
		JsonNode dto = mapper.readTree(mvc.perform(get("/api/calendar/entries/" + id).with(loginAs(a)))
				.andReturn().getResponse().getContentAsString());
		assertThat(dto.get("endDate").asText()).isEqualTo("2026-11-16");
		assertThat(List.of(dto.get("allDay").asBoolean())).containsExactly(true);
	}

	private static Object toTs(Object v) {
		if (v instanceof java.sql.Timestamp) {
			return v;
		}
		if (v instanceof OffsetDateTime o) {
			return java.sql.Timestamp.from(o.toInstant());
		}
		return java.sql.Timestamp.from(((java.time.Instant) v));
	}

}
