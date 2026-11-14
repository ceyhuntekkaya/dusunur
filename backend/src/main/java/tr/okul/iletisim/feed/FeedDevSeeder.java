package tr.okul.iletisim.feed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

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
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.feed.FeedDtos.CommentRequest;
import tr.okul.iletisim.feed.FeedDtos.CreatePostRequest;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FeedDevSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(FeedDevSeeder.class);

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
	private final FeedService service;
	private final FeedInteractionService interactions;
	private final FileStorageService files;

	public FeedDevSeeder(JdbcClient jdbc, FeedService service, FeedInteractionService interactions,
			FileStorageService files) {
		this.jdbc = jdbc;
		this.service = service;
		this.interactions = interactions;
		this.files = files;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (jdbc.sql("SELECT count(*) FROM feed_post").query(Long.class).single() > 0) {
			return;
		}
		Season season = jdbc.sql("SELECT id, name FROM season WHERE status = 'ACTIVE'").query(Season.class)
				.optional().orElse(null);
		if (season == null || !exists("mudur@okul.test")) {
			return;
		}
		UserContext mudur = ctx("mudur@okul.test", season);
		UserContext kuzey = ctx("my.kuzey@okul.test", season);
		UserContext zumre = ctx("zumre.mat@okul.test", season);
		UserContext ali = ctx("ogretmen1@okul.test", season);

		var ilk = service.create(mudur, new CreatePostRequest(List.of(new TargetRef(TargetType.ALL, null)),
				"Öğretmenler Kurulu 20 Kasım Perşembe 15:00'te konferans salonunda yapılacaktır. Katılım zorunludur.",
				List.of(), true));
		service.pin(mudur, ilk.post().id());

		UUID image = upload(kuzey, "ornek-gorsel.png");
		service.create(kuzey, new CreatePostRequest(List.of(new TargetRef(TargetType.BRANCH, branchId("Kuzey Kampüs"))),
				"Kuzey Kampüs'te yarın bahçe düzenleme çalışması yapılacaktır. Sınıf dışı etkinlikler spor salonunda olacaktır.",
				List.of(image), false));

		UUID pdf = upload(zumre, "ornek-belge.pdf");
		var mat = service.create(zumre, new CreatePostRequest(
				List.of(new TargetRef(TargetType.GROUP, groupId("Matematik Zümresi"))),
				"Zümre toplantısı notları ekte. Sınav takvimini gözden geçirip görüşlerinizi yazın.", List.of(pdf),
				false));
		interactions.addComment(ali, mat.post().id(), new CommentRequest("Teşekkürler, notları inceledim."));
		interactions.like(ali, mat.post().id());

		service.create(ali, new CreatePostRequest(List.of(new TargetRef(TargetType.USER, userId("ogretmen2@okul.test"))),
				"Merhaba Zeynep Hanım, paylaştığım kaynaklara www.eba.gov.tr adresinden ulaşabilirsiniz.", List.of(),
				false));

		service.create(mudur, new CreatePostRequest(
				List.of(new TargetRef(TargetType.GROUP, groupId("Yönetim Kurulu"))),
				"Yönetim Kurulu toplantısı gündemi güncellendi. Lütfen okuyup onaylayın.", List.of(), true));
		log.info("Akış geliştirme verisi yüklendi.");
	}

	private boolean exists(String email) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE email = :e)").param("e", email)
				.query(Boolean.class).single();
	}

	private long userId(String email) {
		return jdbc.sql("SELECT id FROM app_user WHERE email = :e").param("e", email).query(Long.class).single();
	}

	private long branchId(String name) {
		return jdbc.sql("SELECT id FROM branch WHERE name = :n").param("n", name).query(Long.class).single();
	}

	private long groupId(String name) {
		return jdbc.sql("SELECT id FROM staff_group WHERE name = :n").param("n", name).query(Long.class).single();
	}

	private UUID upload(UserContext author, String resource) throws IOException {
		byte[] bytes = new ClassPathResource("dev/" + resource).getContentAsByteArray();
		return files.store(author, new BytesFile(resource, bytes), FilePurpose.FEED).getId();
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
