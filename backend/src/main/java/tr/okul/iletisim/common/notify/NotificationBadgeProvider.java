package tr.okul.iletisim.common.notify;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.spi.BadgeProvider;

@Component
public class NotificationBadgeProvider implements BadgeProvider {

	private final JdbcClient jdbc;

	public NotificationBadgeProvider(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Map<String, Integer> badges(UserContext me) {
		int unread = jdbc.sql("SELECT count(*) FROM notification WHERE user_id = :uid AND read_at IS NULL")
				.param("uid", me.userId()).query(Integer.class).single();
		return Map.of("notifications", unread);
	}

}
