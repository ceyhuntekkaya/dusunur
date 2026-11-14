package tr.okul.iletisim.print;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.print.PrintDtos.CreatePrintRequest;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PrintDevSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PrintDevSeeder.class);

	private record Season(long id, String name) {
	}

	private record UserRow(long id, String firstName, String lastName, boolean systemAdmin, Long membershipId,
			Long branchId, String branchName, String title, Boolean manager, Boolean printOperator, Boolean dms,
			Long approverId) {
	}

	private static final class BytesFile implements MultipartFile {

		private final String name;
		private final byte[] bytes;

		BytesFile(String name, byte[] bytes) {
			this.name = name;
			this.bytes = bytes;
		}

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
	private final PrintRequestService service;
	private final FileStorageService files;

	public PrintDevSeeder(JdbcClient jdbc, PrintRequestService service, FileStorageService files) {
		this.jdbc = jdbc;
		this.service = service;
		this.files = files;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (jdbc.sql("SELECT count(*) FROM print_request").query(Long.class).single() > 0) {
			return;
		}
		Season season = jdbc.sql("SELECT id, name FROM season WHERE status = 'ACTIVE'").query(Season.class)
				.optional().orElse(null);
		if (season == null || !exists("mudur@okul.test")) {
			return;
		}
		UserContext ali = ctx("ogretmen1@okul.test", season);
		UserContext zeynep = ctx("ogretmen2@okul.test", season);
		UserContext burak = ctx("ogretmen3@okul.test", season);
		UserContext zumre = ctx("zumre.mat@okul.test", season);
		UserContext kuzeyMd = ctx("my.kuzey@okul.test", season);
		UserContext merkezOp = ctx("fotokopi.merkez@okul.test", season);
		UserContext kuzeyOp = ctx("fotokopi.kuzey@okul.test", season);

		var pdf = files.store(ali, new BytesFile("ornek-belge.pdf",
				new ClassPathResource("dev/ornek-belge.pdf").getContentAsByteArray()), FilePurpose.PRINT);
		service.create(ali, request("9-A Matematik 2. ünite çalışma kâğıdı", pdf.getId(), false, 12, 30, PrintColor.BW,
				PrintSides.DOUBLE, PaperSize.A4, false));

		var ready = service.create(ali, request("10-B Fizik deneme sınavı cevap anahtarı", null, true, 4, 25,
				PrintColor.BW, PrintSides.SINGLE, PaperSize.A4, false));
		service.approve(zumre, ready.id());
		service.ready(merkezOp, ready.id());

		service.create(burak, request("Fen Bilimleri laboratuvar föyü", null, true, 6, 28, PrintColor.BW,
				PrintSides.DOUBLE, PaperSize.A4, false));

		var progress = service.create(zeynep, request("Okul gezisi harita ve program afişi", null, true, 2, 40,
				PrintColor.COLOR, PrintSides.SINGLE, PaperSize.A3, false));
		service.approve(kuzeyMd, progress.id());
		service.start(kuzeyOp, progress.id());

		service.create(zumre, request("Matematik ortak sınav soruları", null, true, 3, 120, PrintColor.BW,
				PrintSides.DOUBLE, PaperSize.A4, true));

		var rejected = service.create(zeynep, request("Türkçe okuma parçaları seti", null, true, 150, 40,
				PrintColor.BW, PrintSides.DOUBLE, PaperSize.A4, false));
		service.reject(kuzeyMd, rejected.id(), "Lütfen sayfa sayısını kontrol edin.");

		var thisMonth = delivered(ali, zumre, merkezOp, "Haftalık değerlendirme formu", 2, 30);
		var lastMonth = delivered(ali, zumre, merkezOp, "Dönem sonu değerlendirme formu", 3, 30);
		jdbc.sql("""
				UPDATE print_request
				SET delivered_at = now() - interval '1 month', ready_at = now() - interval '1 month'
				WHERE id = :id
				""").param("id", lastMonth).update();
		log.info("Fotokopi geliştirme verisi yüklendi ({}, {}).", thisMonth, lastMonth);
	}

	private long delivered(UserContext requester, UserContext approver, UserContext operator, String title, int pages,
			int copies) {
		var r = service.create(requester, request(title, null, true, pages, copies, PrintColor.BW, PrintSides.SINGLE,
				PaperSize.A4, false));
		service.approve(approver, r.id());
		service.ready(operator, r.id());
		service.deliver(operator, r.id());
		return r.id();
	}

	private CreatePrintRequest request(String title, java.util.UUID fileId, boolean paper, int pages, int copies,
			PrintColor color, PrintSides sides, PaperSize size, boolean secret) {
		return new CreatePrintRequest(title, fileId, paper, pages, copies, color, sides, size, secret,
				OffsetDateTime.now(ZoneOffset.UTC).plusDays(2), null, null);
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
