package tr.okul.iletisim.chat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.chat.ChatDtos.AddMembersRequest;
import tr.okul.iletisim.chat.ChatDtos.ConversationDetail;
import tr.okul.iletisim.chat.ChatDtos.ConversationRow;
import tr.okul.iletisim.chat.ChatDtos.CreateGroupRequest;
import tr.okul.iletisim.chat.ChatDtos.RenameRequest;
import tr.okul.iletisim.chat.ChatQueries.ConvRef;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;

@Service
public class ChatConversationService {

	private static final int MAX_MEMBERS = 200;

	private final ChatConversationRepository conversations;
	private final ChatMemberRepository members;
	private final ChatQueries queries;
	private final RealtimePublisher realtime;
	private final JdbcClient jdbc;

	public ChatConversationService(ChatConversationRepository conversations, ChatMemberRepository members,
			ChatQueries queries, RealtimePublisher realtime, JdbcClient jdbc) {
		this.conversations = conversations;
		this.members = members;
		this.queries = queries;
		this.realtime = realtime;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<ConversationRow> list(UserContext me) {
		me.requireMember();
		return queries.conversations(me.userId());
	}

	@Transactional(readOnly = true)
	public ConversationDetail detail(UserContext me, long id) {
		me.requireMember();
		return queries.detail(me, id);
	}

	@Transactional
	public ConversationRow openDirect(UserContext me, long userId) {
		me.requireMember();
		if (userId == me.userId()) {
			throw new BusinessException(ChatErrorCode.CHAT_DIRECT_SELF);
		}
		if (!queries.isActiveUser(userId)) {
			throw new BusinessException(ChatErrorCode.CHAT_USER_INACTIVE);
		}
		String key = Math.min(me.userId(), userId) + ":" + Math.max(me.userId(), userId);
		Long id = jdbc.sql("""
				INSERT INTO chat_conversation (type, direct_key, created_by) VALUES ('DIRECT', :k, :uid)
				ON CONFLICT (direct_key) DO NOTHING RETURNING id
				""").param("k", key).param("uid", me.userId()).query(Long.class).optional().orElse(null);
		if (id == null) {
			id = jdbc.sql("SELECT id FROM chat_conversation WHERE direct_key = :k").param("k", key)
					.query(Long.class).single();
		}
		for (long uid : new long[] { me.userId(), userId }) {
			jdbc.sql("""
					INSERT INTO chat_member (conversation_id, user_id) VALUES (:c, :u) ON CONFLICT DO NOTHING
					""").param("c", id).param("u", uid).update();
		}
		return queries.row(me.userId(), id);
	}

	@Transactional
	public ConversationRow createGroup(UserContext me, CreateGroupRequest request) {
		long seasonId = me.requireMember();
		Set<Long> ids = new LinkedHashSet<>();
		for (Long m : request.memberIds()) {
			if (m != null && m != me.userId()) {
				ids.add(m);
			}
		}
		if (ids.size() < 2) {
			throw new BusinessException(ChatErrorCode.CHAT_GROUP_TOO_SMALL);
		}
		if (ids.size() > MAX_MEMBERS - 1) {
			throw new BusinessException(ChatErrorCode.CHAT_GROUP_TOO_LARGE);
		}
		if (queries.validUserCount(ids, seasonId) != ids.size()) {
			throw new BusinessException(ChatErrorCode.CHAT_MEMBER_INVALID);
		}
		ChatConversation c = new ChatConversation();
		c.setType(ConversationType.FREE);
		c.setTitle(request.title());
		c.setCreatedBy(me.userId());
		c = conversations.saveAndFlush(c);
		List<ChatMember> rows = new ArrayList<>();
		rows.add(new ChatMember(c.getId(), me.userId(), ChatRole.OWNER, 0));
		for (Long uid : ids) {
			rows.add(new ChatMember(c.getId(), uid, ChatRole.MEMBER, 0));
		}
		members.saveAllAndFlush(rows);
		notifyChanged(rows.stream().map(ChatMember::getUserId).toList(), c.getId());
		return queries.row(me.userId(), c.getId());
	}

	@Transactional
	public ConversationDetail rename(UserContext me, long id, RenameRequest request) {
		me.requireMember();
		requireManage(queries.requireAccess(id, me.userId()));
		ChatConversation c = conversations.findById(id)
				.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_CONVERSATION_NOT_FOUND));
		c.setTitle(request.title());
		conversations.saveAndFlush(c);
		notifyChanged(queries.memberIds(id), id);
		return queries.detail(me, id);
	}

	@Transactional
	public ConversationDetail addMembers(UserContext me, long id, AddMembersRequest request) {
		long seasonId = me.requireMember();
		ConvRef ref = queries.requireAccess(id, me.userId());
		requireManage(ref);
		List<Long> existing = queries.memberIds(id);
		Set<Long> added = new LinkedHashSet<>();
		for (Long u : request.userIds()) {
			if (u != null && !existing.contains(u)) {
				added.add(u);
			}
		}
		if (!added.isEmpty()) {
			if (queries.validUserCount(added, seasonId) != added.size()) {
				throw new BusinessException(ChatErrorCode.CHAT_MEMBER_INVALID);
			}
			if (existing.size() + added.size() > MAX_MEMBERS) {
				throw new BusinessException(ChatErrorCode.CHAT_GROUP_TOO_LARGE);
			}
			long lastRead = ref.lastMessageId() == null ? 0 : ref.lastMessageId();
			List<ChatMember> rows = new ArrayList<>();
			for (Long uid : added) {
				rows.add(new ChatMember(id, uid, ChatRole.MEMBER, lastRead));
			}
			members.saveAllAndFlush(rows);
			List<Long> all = new ArrayList<>(existing);
			all.addAll(added);
			notifyChanged(all, id);
		}
		return queries.detail(me, id);
	}

	@Transactional
	public void removeMember(UserContext me, long id, long userId) {
		me.requireMember();
		requireManage(queries.requireAccess(id, me.userId()));
		if (userId == me.userId()) {
			throw new BusinessException(ChatErrorCode.CHAT_USE_LEAVE);
		}
		if (!queries.memberIds(id).contains(userId)) {
			return;
		}
		removeAndRebalance(id, userId);
		notifyChanged(withUser(queries.memberIds(id), userId), id);
		realtime.toUsers(List.of(userId), "BADGES", Map.of());
	}

	@Transactional
	public void leave(UserContext me, long id) {
		me.requireMember();
		ConvRef ref = queries.requireAccess(id, me.userId());
		if (ref.type() != ConversationType.FREE) {
			throw new BusinessException(ChatErrorCode.CHAT_LEAVE_NOT_ALLOWED);
		}
		removeAndRebalance(id, me.userId());
		notifyChanged(withUser(queries.memberIds(id), me.userId()), id);
		realtime.toUsers(List.of(me.userId()), "BADGES", Map.of());
	}

	@Transactional
	public void mute(UserContext me, long id, boolean muted) {
		me.requireMember();
		queries.requireAccess(id, me.userId());
		jdbc.sql("UPDATE chat_member SET muted = :m WHERE conversation_id = :c AND user_id = :u")
				.param("m", muted).param("c", id).param("u", me.userId()).update();
		realtime.toUsers(List.of(me.userId()), "BADGES", Map.of());
	}

	@Transactional
	public void removeUserFromFreeChats(long userId) {
		List<Long> ids = jdbc.sql("""
				SELECT c.id FROM chat_member cm JOIN chat_conversation c ON c.id = cm.conversation_id
				WHERE cm.user_id = :u AND c.type = 'FREE'
				""").param("u", userId).query(Long.class).list();
		for (Long id : ids) {
			removeAndRebalance(id, userId);
			notifyChanged(withUser(queries.memberIds(id), userId), id);
		}
	}

	private void removeAndRebalance(long conversationId, long userId) {
		jdbc.sql("DELETE FROM chat_member WHERE conversation_id = :c AND user_id = :u")
				.param("c", conversationId).param("u", userId).update();
		if (queries.memberIds(conversationId).isEmpty()) {
			jdbc.sql("UPDATE chat_conversation SET read_only = true, updated_at = now() WHERE id = :c")
					.param("c", conversationId).update();
			return;
		}
		boolean hasOwner = jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM chat_member WHERE conversation_id = :c AND role = 'OWNER')
				""").param("c", conversationId).query(Boolean.class).single();
		if (!hasOwner) {
			Long successor = jdbc.sql("""
					SELECT user_id FROM chat_member WHERE conversation_id = :c ORDER BY joined_at, user_id LIMIT 1
					""").param("c", conversationId).query(Long.class).single();
			jdbc.sql("UPDATE chat_member SET role = 'OWNER' WHERE conversation_id = :c AND user_id = :u")
					.param("c", conversationId).param("u", successor).update();
		}
	}

	private void requireManage(ConvRef ref) {
		if (ref.type() != ConversationType.FREE || ref.role() != ChatRole.OWNER) {
			throw new BusinessException(ChatErrorCode.CHAT_MANAGE_FORBIDDEN);
		}
	}

	private void notifyChanged(List<Long> userIds, long conversationId) {
		realtime.toUsers(userIds, "CHAT_CONVERSATION_CHANGED", Map.of("conversationId", conversationId));
	}

	private static List<Long> withUser(List<Long> ids, long userId) {
		List<Long> all = new ArrayList<>(ids);
		all.add(userId);
		return all;
	}

}
