package tr.okul.iletisim.calendar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import tr.okul.iletisim.calendar.CalendarDtos.CalendarRequest;
import tr.okul.iletisim.calendar.CalendarDtos.EntryInput;
import tr.okul.iletisim.calendar.CalendarDtos.ShareCreateRequest;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class CalendarDevSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CalendarDevSeeder.class);

	private record Season(long id, String name) {
	}

	private record UserRow(long id, String firstName, String lastName, boolean systemAdmin, Long membershipId,
			Long branchId, String branchName, String title, Boolean manager, Boolean printOperator, Boolean dms,
			Long approverId) {
	}

	private record BytesFile(String name, byte[] bytes) implements MultipartFile {

		@Override
		public String getName() {
			return "file";
		}

		@Override
		public String getOriginalFilename() {
			return name;
		}

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public boolean isEmpty() {
			return bytes.length == 0;
		}

		@Override
		public long getSize() {
			return bytes.length;
		}

		@Override
		public byte[] getBytes() {
			return bytes;
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public void transferTo(java.io.File dest) throws IOException {
			java.nio.file.Files.write(dest.toPath(), bytes);
		}

	}

	private final JdbcClient jdbc;
	private final CalendarBootstrap bootstrap;
	private final CalendarService calendars;
	private final CalendarEntryService entries;
	private final FileStorageService files;

	public CalendarDevSeeder(JdbcClient jdbc, CalendarBootstrap bootstrap, CalendarService calendars,
			CalendarEntryService entries, FileStorageService files) {
		this.jdbc = jdbc;
		this.bootstrap = bootstrap;
		this.calendars = calendars;
		this.entries = entries;
		this.files = files;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (jdbc.sql("SELECT count(*) FROM cal_entry").query(Long.class).single() > 0) {
			return;
		}
		Season season = jdbc.sql("SELECT id, name FROM season WHERE status = 'ACTIVE'").query(Season.class)
				.optional().orElse(null);
		if (season == null || !exists("mudur@okul.test") || !exists("ogretmen1@okul.test")) {
			return;
		}
		UserContext mudur = ctx("mudur@okul.test", season);
		UserContext ali = ctx("ogretmen1@okul.test", season);
		bootstrap.ensureSystem();
		bootstrap.ensure(ali);
		long institution = idOf("SELECT id FROM cal_calendar WHERE type = 'INSTITUTION'");
		long merkez = idOf("SELECT c.id FROM cal_calendar c JOIN branch b ON b.id = c.branch_id WHERE b.name = 'Merkez Kampüs'");
		long kuzey = idOf("SELECT c.id FROM cal_calendar c JOIN branch b ON b.id = c.branch_id WHERE b.name = 'Kuzey Kampüs'");
		long aliDefault = idOf("SELECT c.id FROM cal_calendar c JOIN app_user u ON u.id = c.owner_id WHERE c.is_default AND u.email = 'ogretmen1@okul.test'");
		long mathGroup = idOf("SELECT id FROM staff_group WHERE name = 'Matematik Zümresi'");
		jdbc.sql("INSERT INTO staff_group_member (group_id, user_id) SELECT :g, id FROM app_user WHERE email = 'zumre.mat@okul.test' ON CONFLICT DO NOTHING")
				.param("g", mathGroup).update();
		UserContext elif = ctx("zumre.mat@okul.test", season);
		long mathCalendar = idOf("SELECT id FROM cal_calendar WHERE type = 'GROUP' AND staff_group_id = " + mathGroup);
		long merkezBranch = idOf("SELECT id FROM branch WHERE name = 'Merkez Kampüs'");
		long kuzeyBranch = idOf("SELECT id FROM branch WHERE name = 'Kuzey Kampüs'");
		byte[] pdf = new ClassPathResource("dev/ornek-belge.pdf").getContentAsByteArray();

		LocalDate month = LocalDate.now(CalendarQueries.ZONE).withDayOfMonth(1);
		LocalDate next = month.plusMonths(1);

		timed(mudur, institution, "Öğretmenler Kurulu", month.plusDays(9), 14, 0, 15, 30, "Konferans Salonu");
		allDay(mudur, institution, "Ara Tatil", next.plusDays(9), next.plusDays(13));
		var stored = files.store(mudur, new BytesFile("toplanti-gundemi.pdf", pdf), FilePurpose.CALENDAR);
		entries.create(mudur, new EntryInput(institution, "Eğitim Yılı Planı", true, month.plusDays(19),
				month.plusDays(19), null, null, null, "Takvim dokümanı ektedir.", stored.getId(), null));
		timed(mudur, merkez, "Veli Toplantısı – 9. sınıflar", next.plusDays(2), 18, 0, 19, 30, null);
		allDay(mudur, kuzey, "Kampüs Gezisi", next.plusDays(4), next.plusDays(4));

		timed(ali, aliDefault, "Rehberlik görüşmesi", month.plusDays(11), 10, 0, 10, 40, null);
		timed(ali, aliDefault, "Ders hazırlığı", next.plusDays(6), 9, 0, 11, 0, null);
		long duty = calendars.createPersonal(ali, new CalendarRequest("Nöbet", null, null)).id();
		timed(ali, duty, "Bahçe nöbeti", month.plusDays(14), 8, 0, 9, 0, null);
		calendars.addShare(ali, duty, new ShareCreateRequest(new TargetRef(TargetType.GROUP, mathGroup), CalShareLevel.VIEW));

		timed(elif, mathCalendar, "Zümre toplantısı", month.plusDays(16), 15, 0, 16, 0, null);

		long hall = calendars.createResource(mudur, new CalendarRequest("Konferans Salonu", null, merkezBranch)).id();
		long lab = calendars.createResource(mudur, new CalendarRequest("Fen Laboratuvarı", null, kuzeyBranch)).id();
		timed(mudur, hall, "Seminer", month.plusDays(12), 10, 0, 11, 0, null);
		timed(mudur, hall, "Tören provası", month.plusDays(12), 11, 0, 12, 0, null);
		timed(mudur, lab, "Deney dersi", month.plusDays(13), 13, 0, 14, 30, null);
		log.info("Takvim geliştirme verisi oluşturuldu.");
	}

	private void timed(UserContext me, long calendarId, String title, LocalDate day, int sh, int sm, int eh, int em,
			String location) {
		OffsetDateTime start = LocalDateTime.of(day, java.time.LocalTime.of(sh, sm)).atZone(CalendarQueries.ZONE)
				.toOffsetDateTime();
		OffsetDateTime end = LocalDateTime.of(day, java.time.LocalTime.of(eh, em)).atZone(CalendarQueries.ZONE)
				.toOffsetDateTime();
		entries.create(me, new EntryInput(calendarId, title, false, null, null, start, end, location, null, null, null));
	}

	private void allDay(UserContext me, long calendarId, String title, LocalDate from, LocalDate to) {
		entries.create(me, new EntryInput(calendarId, title, true, from, to, null, null, null, null, null, null));
	}

	private long idOf(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

	private boolean exists(String email) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE email = :e)").param("e", email)
				.query(Boolean.class).single();
	}

	private UserContext ctx(String email, Season season) {
		UserRow u = jdbc.sql("""
				SELECT u.id, u.first_name, u.last_name, u.system_admin, m.id AS membership_id, m.branch_id,
				       b.name AS branch_name, m.title, m.is_manager AS manager, m.is_print_operator AS print_operator,
				       m.can_create_dms_space AS dms, m.print_approver_id AS approver_id
				FROM app_user u
				LEFT JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				LEFT JOIN branch b ON b.id = m.branch_id
				WHERE u.email = :e
				""").param("sid", season.id()).param("e", email).query(UserRow.class).single();
		var groups = new HashSet<>(jdbc.sql("SELECT group_id FROM staff_group_member WHERE user_id = :uid")
				.param("uid", u.id()).query(Long.class).list());
		UserContext.Membership membership = u.membershipId() == null ? null
				: new UserContext.Membership(u.membershipId(), u.branchId(), u.branchName(), u.title(),
						Boolean.TRUE.equals(u.manager()), Boolean.TRUE.equals(u.printOperator()),
						Boolean.TRUE.equals(u.dms()), u.approverId());
		return new UserContext(u.id(), u.firstName(), u.lastName(), u.systemAdmin(), season.id(), season.name(),
				membership, groups);
	}

}
