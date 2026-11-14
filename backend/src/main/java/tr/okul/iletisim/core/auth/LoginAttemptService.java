package tr.okul.iletisim.core.auth;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.core.user.AppUserRepository;

@Service
public class LoginAttemptService {

	private final AppUserRepository users;
	private final AuditService audit;
	private final AppProperties app;

	public LoginAttemptService(AppUserRepository users, AuditService audit, AppProperties app) {
		this.users = users;
		this.audit = audit;
		this.app = app;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logFailed(Long userId, String email) {
		audit.log(userId, CoreAuditAction.AUTH_LOGIN_FAILED, "USER", userId, Map.of("email", email));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int registerWrongPassword(long userId, String email) {
		var user = users.findById(userId).orElseThrow();
		int count = user.getFailedLoginCount() + 1;
		int lockedMinutes = 0;
		if (count >= app.auth().lockThreshold()) {
			user.setFailedLoginCount(0);
			user.setLockedUntil(OffsetDateTime.now().plusMinutes(app.auth().lockMinutes()));
			lockedMinutes = app.auth().lockMinutes();
		}
		else {
			user.setFailedLoginCount(count);
		}
		users.save(user);
		audit.log(userId, CoreAuditAction.AUTH_LOGIN_FAILED, "USER", userId, Map.of("email", email));
		if (lockedMinutes > 0) {
			audit.log(userId, CoreAuditAction.AUTH_ACCOUNT_LOCKED, "USER", userId, Map.of("email", email));
		}
		return lockedMinutes;
	}

}
