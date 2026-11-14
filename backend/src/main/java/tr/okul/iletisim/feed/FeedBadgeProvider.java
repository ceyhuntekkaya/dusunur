package tr.okul.iletisim.feed;

import java.util.Map;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.spi.BadgeProvider;

@Component
public class FeedBadgeProvider implements BadgeProvider {

	private final FeedQueries queries;

	public FeedBadgeProvider(FeedQueries queries) {
		this.queries = queries;
	}

	@Override
	public Map<String, Integer> badges(UserContext me) {
		if (!me.isMember() || me.seasonId() == null) {
			return Map.of("feedImportant", 0);
		}
		return Map.of("feedImportant", queries.unreadImportantCount(me.seasonId(), me.userId()));
	}

}
