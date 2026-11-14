package tr.okul.iletisim.chat;

import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.core.spi.GroupArchivedEvent;
import tr.okul.iletisim.core.spi.GroupCreatedEvent;
import tr.okul.iletisim.core.spi.GroupMembersAddedEvent;
import tr.okul.iletisim.core.spi.GroupMembersRemovedEvent;
import tr.okul.iletisim.core.spi.GroupUpdatedEvent;
import tr.okul.iletisim.core.spi.UserDeactivatedEvent;

@Component
public class ChatGroupSync {

	private record MissingGroup(long id, String name, boolean archived) {
	}

	private final JdbcClient jdbc;
	private final RealtimePublisher realtime;
	private final ChatConversationService conversationService;

	public ChatGroupSync(JdbcClient jdbc, RealtimePublisher realtime, ChatConversationService conversationService) {
		this.jdbc = jdbc;
		this.realtime = realtime;
		this.conversationService = conversationService;
	}

	@EventListener
	public void onCreated(GroupCreatedEvent e) {
		create(e.groupId(), e.name(), false, e.memberIds());
	}

	@EventListener
	public void onUpdated(GroupUpdatedEvent e) {
		jdbc.sql("UPDATE chat_conversation SET title = :t, updated_at = now() WHERE staff_group_id = :g")
				.param("t", e.name()).param("g", e.groupId()).update();
	}

	@EventListener
	public void onMembersAdded(GroupMembersAddedEvent e) {
		Long cid = conversationId(e.groupId());
		if (cid == null) {
			return;
		}
		for (Long uid : e.userIds()) {
			jdbc.sql("""
					INSERT INTO chat_member (conversation_id, user_id, last_read_message_id)
					VALUES (:c, :u, COALESCE((SELECT last_message_id FROM chat_conversation WHERE id = :c), 0))
					ON CONFLICT DO NOTHING
					""").param("c", cid).param("u", uid).update();
		}
		changed(e.userIds(), cid);
	}

	@EventListener
	public void onMembersRemoved(GroupMembersRemovedEvent e) {
		Long cid = conversationId(e.groupId());
		if (cid == null) {
			return;
		}
		for (Long uid : e.userIds()) {
			jdbc.sql("DELETE FROM chat_member WHERE conversation_id = :c AND user_id = :u")
					.param("c", cid).param("u", uid).update();
		}
		changed(e.userIds(), cid);
	}

	@EventListener
	public void onArchived(GroupArchivedEvent e) {
		Long cid = conversationId(e.groupId());
		if (cid == null) {
			return;
		}
		jdbc.sql("UPDATE chat_conversation SET read_only = :r, updated_at = now() WHERE id = :c")
				.param("r", e.archived()).param("c", cid).update();
		changed(jdbc.sql("SELECT user_id FROM chat_member WHERE conversation_id = :c").param("c", cid)
				.query(Long.class).list(), cid);
	}

	@EventListener
	public void onUserDeactivated(UserDeactivatedEvent e) {
		conversationService.removeUserFromFreeChats(e.userId());
	}

	@Transactional
	public void syncMissing() {
		List<MissingGroup> groups = jdbc.sql("""
				SELECT g.id, g.name, g.archived FROM staff_group g
				WHERE NOT EXISTS (SELECT 1 FROM chat_conversation c WHERE c.staff_group_id = g.id)
				""").query(MissingGroup.class).list();
		for (MissingGroup g : groups) {
			List<Long> memberIds = jdbc.sql("SELECT user_id FROM staff_group_member WHERE group_id = :g")
					.param("g", g.id()).query(Long.class).list();
			create(g.id(), g.name(), g.archived(), memberIds);
		}
	}

	private void create(long groupId, String name, boolean archived, List<Long> memberIds) {
		if (conversationId(groupId) != null) {
			return;
		}
		Long cid = jdbc.sql("""
				INSERT INTO chat_conversation (type, title, staff_group_id, read_only)
				VALUES ('GROUP', :t, :g, :r) RETURNING id
				""").param("t", name).param("g", groupId).param("r", archived).query(Long.class).single();
		for (Long uid : memberIds) {
			jdbc.sql("INSERT INTO chat_member (conversation_id, user_id) VALUES (:c, :u) ON CONFLICT DO NOTHING")
					.param("c", cid).param("u", uid).update();
		}
	}

	private Long conversationId(long groupId) {
		return jdbc.sql("SELECT id FROM chat_conversation WHERE staff_group_id = :g").param("g", groupId)
				.query(Long.class).optional().orElse(null);
	}

	private void changed(List<Long> userIds, long conversationId) {
		realtime.toUsers(userIds, "CHAT_CONVERSATION_CHANGED", Map.of("conversationId", conversationId));
	}

}
