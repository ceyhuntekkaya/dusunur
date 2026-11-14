package tr.okul.iletisim.chat;

import java.util.Map;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.spi.BadgeProvider;

@Component
public class ChatBadgeProvider implements BadgeProvider {

	private final ChatQueries queries;

	public ChatBadgeProvider(ChatQueries queries) {
		this.queries = queries;
	}

	@Override
	public Map<String, Integer> badges(UserContext me) {
		if (!me.isMember()) {
			return Map.of("chat", 0);
		}
		return Map.of("chat", queries.unreadConversationCount(me.userId()));
	}

}
