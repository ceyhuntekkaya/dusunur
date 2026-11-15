package tr.okul.iletisim.core.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.mail.MailService;
import tr.okul.iletisim.common.security.SessionService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.auth.AuthDtos.ChangePasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.ForgotPasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.KvkkResponse;
import tr.okul.iletisim.core.auth.AuthDtos.LoginRequest;
import tr.okul.iletisim.core.auth.AuthDtos.MeResponse;
import tr.okul.iletisim.core.auth.AuthDtos.MessageResponse;
import tr.okul.iletisim.core.auth.AuthDtos.SetPasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.TokenInfoResponse;
import tr.okul.iletisim.core.season.Season;
import tr.okul.iletisim.core.season.SeasonMembershipRepository;
import tr.okul.iletisim.core.season.SeasonRepository;
import tr.okul.iletisim.core.season.SeasonStatus;
import tr.okul.iletisim.core.user.AppUser;
import tr.okul.iletisim.core.user.AppUserRepository;
import tr.okul.iletisim.core.user.UserStatus;

@Service
public class AuthService {

	static final String FORGOT_MESSAGE = "Bu e-posta adresi sistemde kayıtlıysa şifre yenileme bağlantısı gönderildi. Lütfen e-postanızı (gereksiz klasörü dahil) kontrol edin.";

	private final AppUserRepository users;
	private final SeasonRepository seasons;
	private final SeasonMembershipRepository memberships;
	private final AuthTokenRepository tokens;
	private final AuthTokenService tokenService;
	private final LoginAttemptService attempts;
	private final PasswordEncoder encoder;
	private final SecurityContextRepository contextRepository;
	private final SessionService sessions;
	private final AuditService audit;
	private final MailService mail;
	private final MeQueries meQueries;
	private final AppProperties app;
	private final String dummyHash;

	public AuthService(AppUserRepository users, SeasonRepository seasons, SeasonMembershipRepository memberships,
			AuthTokenRepository tokens, AuthTokenService tokenService, LoginAttemptService attempts,
			PasswordEncoder encoder, SecurityContextRepository contextRepository, SessionService sessions,
			AuditService audit, MailService mail, MeQueries meQueries, AppProperties app) {
		this.users = users;
		this.seasons = seasons;
		this.memberships = memberships;
		this.tokens = tokens;
		this.tokenService = tokenService;
		this.attempts = attempts;
		this.encoder = encoder;
		this.contextRepository = contextRepository;
		this.sessions = sessions;
		this.audit = audit;
		this.mail = mail;
		this.meQueries = meQueries;
		this.app = app;
		this.dummyHash = encoder.encode("dummy-password-for-timing");
	}

	public MeResponse login(LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
		String email = normalizeEmail(req.email());
		AppUser user = users.findByEmail(email).orElse(null);
		OffsetDateTime now = OffsetDateTime.now();

		if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
			long seconds = Duration.between(now, user.getLockedUntil()).getSeconds();
			attempts.logFailed(user.getId(), email);
			throw new BusinessException(CoreErrorCode.AUTH_ACCOUNT_LOCKED, (seconds + 59) / 60);
		}
		if (user == null) {
			encoder.matches(req.password(), dummyHash);
			attempts.logFailed(null, email);
			throw new BusinessException(CoreErrorCode.AUTH_INVALID_CREDENTIALS);
		}
		if (user.getPasswordHash() == null) {
			attempts.logFailed(user.getId(), email);
			throw new BusinessException(CoreErrorCode.AUTH_PASSWORD_NOT_SET);
		}
		if (!encoder.matches(req.password(), user.getPasswordHash())) {
			int locked = attempts.registerWrongPassword(user.getId(), email);
			if (locked > 0) {
				throw new BusinessException(CoreErrorCode.AUTH_ACCOUNT_LOCKED, locked);
			}
			throw new BusinessException(CoreErrorCode.AUTH_INVALID_CREDENTIALS);
		}
		if (user.getStatus() == UserStatus.PASSIVE) {
			attempts.logFailed(user.getId(), email);
			throw new BusinessException(CoreErrorCode.AUTH_ACCOUNT_PASSIVE);
		}
		if (!user.isSystemAdmin()) {
			Season active = seasons.findByStatus(SeasonStatus.ACTIVE).orElse(null);
			if (active == null) {
				attempts.logFailed(user.getId(), email);
				throw new BusinessException(CoreErrorCode.AUTH_NO_ACTIVE_SEASON);
			}
			if (memberships.findBySeasonIdAndUserId(active.getId(), user.getId()).isEmpty()) {
				attempts.logFailed(user.getId(), email);
				throw new BusinessException(CoreErrorCode.AUTH_NO_SEASON_MEMBERSHIP, active.getName());
			}
		}

		user.setFailedLoginCount(0);
		user.setLockedUntil(null);
		user.setLastLoginAt(now);
		users.save(user);

		request.getSession(true);
		request.changeSessionId();
		var auth = UsernamePasswordAuthenticationToken.authenticated(String.valueOf(user.getId()), null, List.of());
		SecurityContext ctx = SecurityContextHolder.createEmptyContext();
		ctx.setAuthentication(auth);
		SecurityContextHolder.setContext(ctx);
		contextRepository.saveContext(ctx, request, response);

		audit.log(user.getId(), CoreAuditAction.AUTH_LOGIN_SUCCESS, "USER", user.getId(), Map.of());
		return meQueries.me(user.getId());
	}

	public void logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

	public KvkkResponse kvkk() {
		try {
			String text = new ClassPathResource("kvkk/aydinlatma.md").getContentAsString(StandardCharsets.UTF_8);
			return new KvkkResponse(app.kvkk().version(), text);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Transactional
	public void acceptKvkk(UserContext me) {
		AppUser user = users.findById(me.userId()).orElseThrow();
		user.setKvkkVersion(app.kvkk().version());
		user.setKvkkAcceptedAt(OffsetDateTime.now());
		users.save(user);
		audit.log(me.userId(), CoreAuditAction.KVKK_ACCEPTED, "USER", me.userId(), Map.of());
	}

	@Transactional
	public void changePassword(UserContext me, ChangePasswordRequest req, String currentSessionId) {
		AppUser user = users.findById(me.userId()).orElseThrow();
		if (user.getPasswordHash() == null || !encoder.matches(req.currentPassword(), user.getPasswordHash())) {
			throw BusinessException.field(CoreErrorCode.AUTH_CURRENT_PASSWORD_WRONG, "currentPassword");
		}
		if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
			throw BusinessException.field(CoreErrorCode.AUTH_PASSWORD_SAME, "newPassword");
		}
		user.setPasswordHash(encoder.encode(req.newPassword()));
		users.save(user);
		sessions.invalidateAll(user.getId(), currentSessionId);
		audit.log(user.getId(), CoreAuditAction.AUTH_PASSWORD_CHANGED, "USER", user.getId(), Map.of());
	}

	@Transactional
	public MessageResponse forgotPassword(ForgotPasswordRequest req) {
		String email = normalizeEmail(req.email());
		AppUser user = users.findByEmail(email).orElse(null);
		if (user != null && user.getStatus() == UserStatus.ACTIVE) {
			long recent = tokens.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), TokenPurpose.RESET,
					OffsetDateTime.now().minusHours(1));
			if (recent < app.auth().forgotLimitPerHour()) {
				tokenService.invalidateOpen(user.getId());
				String raw = tokenService.issue(user.getId(), TokenPurpose.RESET,
						Duration.ofMinutes(app.auth().resetTokenMinutes()));
				mail.sendResetLink(user.getEmail(), user.getFirstName(), tokenService.link(raw));
			}
		}
		return new MessageResponse(FORGOT_MESSAGE);
	}

	@Transactional(readOnly = true)
	public TokenInfoResponse tokenInfo(String token) {
		AuthToken t = validToken(token);
		AppUser user = users.findById(t.getUserId()).orElseThrow();
		return new TokenInfoResponse(t.getPurpose().name(), user.getFirstName(), user.getEmail());
	}

	@Transactional
	public void setPassword(SetPasswordRequest req) {
		AuthToken t = validToken(req.token());
		AppUser user = users.findById(t.getUserId()).orElseThrow();
		user.setPasswordHash(encoder.encode(req.newPassword()));
		user.setFailedLoginCount(0);
		user.setLockedUntil(null);
		users.save(user);
		tokenService.invalidateOpen(user.getId());
		sessions.invalidateAll(user.getId(), null);
		audit.log(user.getId(), CoreAuditAction.AUTH_PASSWORD_SET, "USER", user.getId(),
				Map.of("purpose", t.getPurpose().name()));
	}

	private static String normalizeEmail(String raw) {
		return raw.replaceAll("[\\p{Cc}\\p{Cf}]", "").trim().toLowerCase(Locale.ROOT);
	}

	private AuthToken validToken(String raw) {
		AuthToken t = raw == null ? null : tokens.findByTokenHash(AuthTokenService.hash(raw)).orElse(null);
		if (t == null || t.getUsedAt() != null || !t.getExpiresAt().isAfter(OffsetDateTime.now())) {
			throw new BusinessException(CoreErrorCode.AUTH_TOKEN_INVALID);
		}
		AppUser user = users.findById(t.getUserId()).orElse(null);
		if (user == null || user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(CoreErrorCode.AUTH_TOKEN_INVALID);
		}
		return t;
	}

}
