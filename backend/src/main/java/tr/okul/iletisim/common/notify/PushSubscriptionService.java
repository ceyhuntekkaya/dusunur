package tr.okul.iletisim.common.notify;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.simple.JdbcClient;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.notify.NotifyDtos.PushSubscriptionRequest;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.CoreErrorCode;

@Service
public class PushSubscriptionService {

	private static final Set<String> ALLOWED_HOSTS = Set.of("fcm.googleapis.com", "web.push.apple.com",
			"updates.push.services.mozilla.com");
	private static final List<String> ALLOWED_SUFFIXES = List.of(".push.services.mozilla.com", ".push.apple.com",
			".notify.windows.com");

	private final PushSubscriptionRepository repository;
	private final JdbcClient jdbc;

	public PushSubscriptionService(PushSubscriptionRepository repository, JdbcClient jdbc) {
		this.repository = repository;
		this.jdbc = jdbc;
	}

	@Transactional
	public void subscribe(UserContext me, PushSubscriptionRequest req, String userAgent) {
		if (req == null || req.keys() == null || blank(req.endpoint()) || blank(req.keys().p256dh())
				|| blank(req.keys().auth()) || req.endpoint().length() > 1000 || !allowedEndpoint(req.endpoint())
				|| req.keys().p256dh().length() > 200 || req.keys().auth().length() > 100) {
			throw new BusinessException(CoreErrorCode.PUSH_SUBSCRIPTION_INVALID);
		}
		PushSubscription s = repository.findByEndpoint(req.endpoint()).orElseGet(PushSubscription::new);
		s.setEndpoint(req.endpoint());
		s.setUserId(me.userId());
		s.setP256dh(req.keys().p256dh());
		s.setAuth(req.keys().auth());
		s.setUserAgent(userAgent == null ? null : userAgent.length() > 300 ? userAgent.substring(0, 300) : userAgent);
		repository.save(s);
	}

	@Transactional
	public void unsubscribe(UserContext me, String endpoint) {
		if (blank(endpoint)) {
			return;
		}
		jdbc.sql("DELETE FROM push_subscription WHERE endpoint = :e AND user_id = :uid")
				.param("e", endpoint).param("uid", me.userId()).update();
	}

	private static boolean allowedEndpoint(String endpoint) {
		String host;
		try {
			URI uri = new URI(endpoint);
			if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null || uri.getPort() != -1) {
				return false;
			}
			host = uri.getHost();
		}
		catch (URISyntaxException e) {
			return false;
		}
		if (host == null) {
			return false;
		}
		String h = host.toLowerCase(Locale.ROOT);
		return ALLOWED_HOSTS.contains(h) || ALLOWED_SUFFIXES.stream().anyMatch(h::endsWith);
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}

}
