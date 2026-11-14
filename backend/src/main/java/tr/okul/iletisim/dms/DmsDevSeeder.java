package tr.okul.iletisim.dms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.dms.DmsDtos.FolderCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.GrantCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.OnConflict;
import tr.okul.iletisim.dms.DmsDtos.SaveFileRequest;
import tr.okul.iletisim.dms.DmsDtos.SpaceRequest;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class DmsDevSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DmsDevSeeder.class);

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
	private final DmsSpaceService spaces;
	private final DmsItemService items;
	private final FileStorageService files;

	public DmsDevSeeder(JdbcClient jdbc, DmsSpaceService spaces, DmsItemService items, FileStorageService files) {
		this.jdbc = jdbc;
		this.spaces = spaces;
		this.items = items;
		this.files = files;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (jdbc.sql("SELECT count(*) FROM dms_space").query(Long.class).single() > 0) {
			return;
		}
		Season season = jdbc.sql("SELECT id, name FROM season WHERE status = 'ACTIVE'").query(Season.class)
				.optional().orElse(null);
		if (season == null || !exists("mudur@okul.test")) {
			return;
		}
		UserContext mudur = ctx("mudur@okul.test", season);
		UserContext zumre = ctx("zumre.mat@okul.test", season);
		UserContext kuzey = ctx("my.kuzey@okul.test", season);
		long boardGroup = idOf("SELECT id FROM staff_group WHERE name = 'Yönetim Kurulu'");
		long mathGroup = idOf("SELECT id FROM staff_group WHERE name = 'Matematik Zümresi'");
		long kuzeyBranch = idOf("SELECT id FROM branch WHERE name = 'Kuzey Kampüs'");
		byte[] pdf = new ClassPathResource("dev/ornek-belge.pdf").getContentAsByteArray();
		byte[] png = new ClassPathResource("dev/ornek-gorsel.png").getContentAsByteArray();

		long forms = spaces.create(mudur, new SpaceRequest("İdari Formlar", "Kurumun idari form ve şablonları")).id();
		spaces.addGrant(mudur, forms, new GrantCreateRequest(new TargetRef(TargetType.ALL, null), DmsLevel.VIEW));
		spaces.addGrant(mudur, forms, new GrantCreateRequest(new TargetRef(TargetType.GROUP, boardGroup), DmsLevel.EDIT));
		long leave = items.createFolder(mudur, forms, new FolderCreateRequest(null, "İzin Formları")).id();
		upload(mudur, forms, leave, "izin-formu.pdf", pdf);
		upload(mudur, forms, null, "okul-logosu.png", png);

		long math = spaces.create(zumre, new SpaceRequest("Matematik Zümresi Arşivi", "Zümre sınav ve tutanak arşivi")).id();
		spaces.addGrant(zumre, math, new GrantCreateRequest(new TargetRef(TargetType.GROUP, mathGroup), DmsLevel.EDIT));
		long exams = items.createFolder(zumre, math, new FolderCreateRequest(null, "2025-2026 Sınavlar")).id();
		long term = items.createFolder(zumre, math, new FolderCreateRequest(exams, "1. Dönem")).id();
		upload(zumre, math, term, "1-donem-yazili-sinavi.pdf", pdf);
		long trashed = upload(zumre, math, null, "eski-taslak.pdf", pdf);
		items.trashFile(zumre, trashed);

		long north = spaces.create(kuzey, new SpaceRequest("Kuzey Kampüs", "Kuzey Kampüs duyuru ve yönergeleri")).id();
		spaces.addGrant(kuzey, north, new GrantCreateRequest(new TargetRef(TargetType.BRANCH, kuzeyBranch), DmsLevel.VIEW));
		upload(kuzey, north, null, "kuzey-kampus-yonergesi.pdf", pdf);
		log.info("Doküman geliştirme verisi oluşturuldu.");
	}

	private long upload(UserContext me, long spaceId, Long folderId, String name, byte[] bytes) {
		var stored = files.store(me, new BytesFile(name, bytes), FilePurpose.DMS);
		return items.saveFile(me, spaceId, new SaveFileRequest(folderId, stored.getId(), OnConflict.ASK)).row().id();
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
