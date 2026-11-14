package tr.okul.iletisim.core.job;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.core.user.AppUser;
import tr.okul.iletisim.core.user.AppUserRepository;
import tr.okul.iletisim.core.user.UserStatus;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

	private final AppUserRepository users;
	private final PasswordEncoder encoder;
	private final AppProperties app;

	public BootstrapAdminRunner(AppUserRepository users, PasswordEncoder encoder, AppProperties app) {
		this.users = users;
		this.encoder = encoder;
		this.app = app;
	}

	@Override
	public void run(ApplicationArguments args) {
		String email = app.bootstrapAdmin().email();
		String password = app.bootstrapAdmin().password();
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			return;
		}
		if (users.countBySystemAdminTrueAndStatus(UserStatus.ACTIVE) > 0) {
			return;
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmail(normalized)) {
			log.warn("Başlangıç sistem yöneticisi oluşturulamadı; bu e-posta zaten kayıtlı: {}", normalized);
			return;
		}
		AppUser admin = new AppUser();
		admin.setEmail(normalized);
		admin.setFirstName("Sistem");
		admin.setLastName("Yöneticisi");
		admin.setSystemAdmin(true);
		admin.setPasswordHash(encoder.encode(password));
		users.save(admin);
		log.info("Başlangıç sistem yöneticisi oluşturuldu: {}", normalized);
	}

}
