package tr.okul.iletisim.core.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;

@Service
public class AuthTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AuthTokenRepository tokens;
	private final AppProperties app;

	public AuthTokenService(AuthTokenRepository tokens, AppProperties app) {
		this.tokens = tokens;
		this.app = app;
	}

	@Transactional
	public String issue(long userId, TokenPurpose purpose, Duration validity) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		AuthToken t = new AuthToken();
		t.setUserId(userId);
		t.setTokenHash(hash(raw));
		t.setPurpose(purpose);
		t.setExpiresAt(OffsetDateTime.now().plus(validity));
		tokens.save(t);
		return raw;
	}

	@Transactional
	public void invalidateOpen(long userId) {
		OffsetDateTime now = OffsetDateTime.now();
		for (AuthToken t : tokens.findByUserIdAndUsedAtIsNull(userId)) {
			t.setUsedAt(now);
		}
	}

	public String link(String rawToken) {
		return app.baseUrl() + "/sifre-belirle?token=" + rawToken;
	}

	public static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
