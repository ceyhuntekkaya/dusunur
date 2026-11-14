package tr.okul.iletisim.common.notify;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.simple.JdbcClient;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.notify.NotifyDtos.PushSubscriptionRequest;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.CoreErrorCode;

@Service
public class PushSubscriptionService {

	private final PushSubscriptionRepository repository;
	private final JdbcClient jdbc;

	public PushSubscriptionService(PushSubscriptionRepository repository, JdbcClient jdbc) {
		this.repository = repository;
		this.jdbc = jdbc;
	}

	@Transactional
	public void subscribe(UserContext me, PushSubscriptionRequest req, String userAgent) {
		if (req == null || req.keys() == null || blank(req.endpoint()) || blank(req.keys().p256dh())
				|| blank(req.keys().auth()) || !req.endpoint().startsWith("https://") || req.endpoint().length() > 1000
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

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}

}
