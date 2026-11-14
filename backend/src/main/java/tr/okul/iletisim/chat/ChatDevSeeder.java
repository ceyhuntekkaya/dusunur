package tr.okul.iletisim.chat;

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

import tr.okul.iletisim.chat.ChatDtos.CreateGroupRequest;
import tr.okul.iletisim.chat.ChatDtos.SendMessageRequest;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;

@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class ChatDevSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ChatDevSeeder.class);

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
	private final ChatConversationService conversations;
	private final ChatMessageService messages;
	private final FileStorageService files;

	public ChatDevSeeder(JdbcClient jdbc, ChatConversationService conversations, ChatMessageService messages,
			FileStorageService files) {
		this.jdbc = jdbc;
		this.conversations = conversations;
		this.messages = messages;
		this.files = files;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (jdbc.sql("SELECT count(*) FROM chat_message").query(Long.class).single() > 0) {
			return;
		}
		Season season = jdbc.sql("SELECT id, name FROM season WHERE status = 'ACTIVE'").query(Season.class)
				.optional().orElse(null);
		if (season == null || !exists("ogretmen1@okul.test") || !exists("ogretmen2@okul.test")) {
			return;
		}
		UserContext ali = ctx("ogretmen1@okul.test", season);
		UserContext zeynep = ctx("ogretmen2@okul.test", season);

		long direct = conversations.openDirect(ali, zeynep.userId()).id();
		send(ali, direct, "Zeynep Hanım merhaba, yarınki ortak sınav için soruları hazırladınız mı?", null, null);
		var yanit = send(zeynep, direct, "Merhaba Ali Bey, hazırladım. Akşama kadar size gönderirim.", null, null);
		UUID image = upload(zeynep, "ornek-gorsel.png");
		send(zeynep, direct, "Cevap anahtarının fotoğrafı ekte.", List.of(image), null);
		send(ali, direct, "Çok teşekkürler, ben de kontrol edeyim.", null, null);
		send(ali, direct, "Bu arada hazırladığınızı yarın sabah çoğaltmaya göndereceğim.", null, yanit);
		var son = send(ali, direct, "Teşekkürler, kolay gelsin.", null, null);
		messages.markRead(zeynep, direct, son);

		Long mat = jdbc.sql("SELECT id FROM chat_conversation WHERE type = 'GROUP' AND title = 'Matematik Zümresi'")
				.query(Long.class).optional().orElse(null);
		if (mat != null) {
			UserContext zumre = ctx("zumre.mat@okul.test", season);
			send(zumre, mat, "Arkadaşlar, dönem sonu sınav takvimini Perşembe'ye kadar netleştirelim.", null, null);
			send(ali, mat, "Ben 9. sınıf sorularını hazırlıyorum.", null, null);
			long yanlis = send(ali, mat, "Sınav pazartesi mi yapılacak?", null, null);
			send(zumre, mat, "Salı günü yapılacak, takvimde güncelledim.", null, null);
			messages.delete(ali, yanlis);
		}

		UserContext kuzey = ctx("my.kuzey@okul.test", season);
		UserContext fotokopi = ctx("fotokopi.kuzey@okul.test", season);
		long gezi = conversations.createGroup(zeynep,
				new CreateGroupRequest("Kuzey Gezi Planı", List.of(kuzey.userId(), fotokopi.userId()))).id();
		send(zeynep, gezi, "Gezi planı taslağını hazırladım, görüşlerinizi bekliyorum.", null, null);
		send(kuzey, gezi, "Otobüs firmasıyla ben görüşeceğim.", null, null);
		send(fotokopi, gezi, "Katılım formlarını çoğaltmaya hazırım.", null, null);
		conversations.mute(kuzey, gezi, true);
		log.info("Mesajlaşma geliştirme verisi yüklendi.");
	}

	private long send(UserContext me, long conversationId, String content, List<UUID> fileIds, Long replyToId) {
		return messages.send(me, conversationId,
				new SendMessageRequest(UUID.randomUUID(), content, fileIds, replyToId)).message().id();
	}

	private boolean exists(String email) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE email = :e)").param("e", email)
				.query(Boolean.class).single();
	}

	private UUID upload(UserContext author, String resource) throws IOException {
		byte[] bytes = new ClassPathResource("dev/" + resource).getContentAsByteArray();
		return files.store(author, new BytesFile(resource, bytes), FilePurpose.CHAT).getId();
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
