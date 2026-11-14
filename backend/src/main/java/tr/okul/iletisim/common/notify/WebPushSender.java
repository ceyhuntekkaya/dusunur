package tr.okul.iletisim.common.notify;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import tools.jackson.databind.json.JsonMapper;
import tr.okul.iletisim.AppProperties;

@Component
public class WebPushSender {

	private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

	private final AppProperties app;
	private final PushSubscriptionRepository subscriptions;
	private final JdbcClient jdbc;
	private final JsonMapper jsonMapper;
	private PushService pushService;

	public WebPushSender(AppProperties app, PushSubscriptionRepository subscriptions, JdbcClient jdbc,
			JsonMapper jsonMapper) {
		this.app = app;
		this.subscriptions = subscriptions;
		this.jdbc = jdbc;
		this.jsonMapper = jsonMapper;
	}

	@PostConstruct
	void init() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		AppProperties.Push push = app.push();
		if (push.vapidPublicKey() == null || push.vapidPublicKey().isBlank() || push.vapidPrivateKey() == null
				|| push.vapidPrivateKey().isBlank()) {
			log.warn("VAPID anahtarları tanımlı değil; Web Push gönderimi kapalı.");
			return;
		}
		try {
			pushService = new PushService(push.vapidPublicKey(), push.vapidPrivateKey(), push.subject());
		}
		catch (Exception e) {
			log.warn("Web Push başlatılamadı; gönderim kapalı: {}", e.getMessage());
		}
	}

	@Async
	public void send(Collection<Long> userIds, String title, String body, String link, String tag) {
		if (pushService == null || userIds.isEmpty()) {
			return;
		}
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("title", title);
		payload.put("body", body == null ? "" : body.length() > 200 ? body.substring(0, 200) : body);
		payload.put("link", link == null ? "" : link);
		payload.put("tag", tag == null ? "" : tag);
		byte[] bytes = jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);

		List<PushSubscription> subs = subscriptions.findByUserIdIn(userIds);
		for (PushSubscription s : subs) {
			try {
				var response = pushService.send(new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), bytes));
				int status = response.getStatusLine().getStatusCode();
				if (status == 404 || status == 410) {
					jdbc.sql("DELETE FROM push_subscription WHERE id = :id").param("id", s.getId()).update();
				}
				else if (status >= 300) {
					log.warn("Web Push yanıtı beklenmedik: {} (abonelik {})", status, s.getId());
				}
			}
			catch (Exception e) {
				log.warn("Web Push gönderilemedi (abonelik {}): {}", s.getId(), e.toString());
			}
		}
	}

}
