package tr.okul.iletisim.core.user;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.mail.MailService;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.core.auth.AuthTokenService;
import tr.okul.iletisim.core.auth.TokenPurpose;

@Component
public class UserImportMailJob {

	private static final Logger log = LoggerFactory.getLogger(UserImportMailJob.class);

	private final AppUserRepository users;
	private final AuthTokenService tokens;
	private final MailService mail;
	private final NotificationService notifications;
	private final AppProperties app;

	public UserImportMailJob(AppUserRepository users, AuthTokenService tokens, MailService mail,
			NotificationService notifications, AppProperties app) {
		this.users = users;
		this.tokens = tokens;
		this.mail = mail;
		this.notifications = notifications;
		this.app = app;
	}

	@Async
	public void run(long actorUserId, List<Long> userIds) {
		int ok = 0;
		int fail = 0;
		for (Long id : userIds) {
			try {
				AppUser u = users.findById(id).orElse(null);
				if (u == null) {
					fail++;
					continue;
				}
				String raw = tokens.issue(id, TokenPurpose.SETUP, Duration.ofHours(app.auth().setupTokenHours()));
				if (mail.sendSetupLink(u.getEmail(), u.getFirstName(), tokens.link(raw), false)) {
					ok++;
				}
				else {
					fail++;
				}
			}
			catch (RuntimeException e) {
				log.error("Toplu yükleme e-postası hazırlanamadı (kullanıcı {})", id, e);
				fail++;
			}
		}
		notifications.notify(List.of(actorUserId), "CORE_IMPORT_MAIL_DONE", "Toplu personel e-postaları gönderildi",
				ok + " e-posta gönderildi, " + fail + " gönderilemedi.", "/yonetim/personel?parola=yok");
	}

}
