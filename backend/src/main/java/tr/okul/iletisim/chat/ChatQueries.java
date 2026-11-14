package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.chat.ChatDtos.ChatAttachment;
import tr.okul.iletisim.chat.ChatDtos.ChatMemberDto;
import tr.okul.iletisim.chat.ChatDtos.ChatPerson;
import tr.okul.iletisim.chat.ChatDtos.ConversationDetail;
import tr.okul.iletisim.chat.ChatDtos.ConversationRow;
import tr.okul.iletisim.chat.ChatDtos.LastMessage;
import tr.okul.iletisim.chat.ChatDtos.MessageDto;
import tr.okul.iletisim.chat.ChatDtos.OtherRef;
import tr.okul.iletisim.chat.ChatDtos.ReplyRef;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;

@Component
public class ChatQueries {

	public record ConvRef(long id, ConversationType type, String title, boolean readOnly, Long staffGroupId,
			Long lastMessageId, ChatRole role, boolean muted) {
	}

	public record MemberFlag(long userId, boolean muted) {
	}

	public record MessageRef(long id, long conversationId, long seasonId, long senderId, OffsetDateTime deletedAt) {
	}

	private record ListRow(long id, ConversationType type, String title, boolean readOnly,
			OffsetDateTime lastMessageAt, boolean muted, long memberCount, Long otherId, String otherFirst,
			String otherLast, String otherStatus, Long lmId, String lmContent, OffsetDateTime lmDeleted,
			Long lmSender, String lmSenderFirst, long unreadCount) {
	}

	private record KindRow(long messageId, String contentType) {
	}

	private record MsgRow(long id, long conversationId, long seasonId, long senderId, String content, Long replyToId,
			UUID clientId, OffsetDateTime createdAt, OffsetDateTime deletedAt, String firstName, String lastName,
			String status, String title, String branchName) {
	}

	private record AttRow(long messageId, UUID fileId, String originalName, String contentType, long sizeBytes) {
	}

	private record ReplyRow(long id, String content, OffsetDateTime deletedAt, String firstName, String lastName) {
	}

	private record MemberRow(long id, String firstName, String lastName, String status, String title,
			String branchName, ChatRole role) {
	}

	private final JdbcClient jdbc;

	public ChatQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public ConvRef requireAccess(long conversationId, long userId) {
		return jdbc.sql("""
				SELECT c.id, c.type, c.title, c.read_only, c.staff_group_id, c.last_message_id, cm.role, cm.muted
				FROM chat_member cm JOIN chat_conversation c ON c.id = cm.conversation_id
				WHERE cm.conversation_id = :cid AND cm.user_id = :uid
				""").param("cid", conversationId).param("uid", userId).query(ConvRef.class).optional()
				.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_CONVERSATION_NOT_FOUND));
	}

	public List<Long> memberIds(long conversationId) {
		return jdbc.sql("SELECT user_id FROM chat_member WHERE conversation_id = :cid")
				.param("cid", conversationId).query(Long.class).list();
	}

	public List<MemberFlag> memberFlags(long conversationId) {
		return jdbc.sql("SELECT user_id, muted FROM chat_member WHERE conversation_id = :cid")
				.param("cid", conversationId).query(MemberFlag.class).list();
	}

	public int validUserCount(Collection<Long> ids, long seasonId) {
		if (ids.isEmpty()) {
			return 0;
		}
		return jdbc.sql("""
				SELECT count(*) FROM app_user u
				JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				WHERE u.id IN (:ids) AND u.status = 'ACTIVE'
				""").param("sid", seasonId).param("ids", ids).query(Integer.class).single();
	}

	public boolean isActiveUser(long userId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM app_user WHERE id = :id AND status = 'ACTIVE')")
				.param("id", userId).query(Boolean.class).single();
	}

	public MessageRef messageRef(long id) {
		return jdbc.sql("""
				SELECT id, conversation_id, season_id, sender_id, deleted_at FROM chat_message WHERE id = :id
				""").param("id", id).query(MessageRef.class).optional().orElse(null);
	}

	public boolean messageInConversation(long messageId, long conversationId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM chat_message WHERE id = :m AND conversation_id = :c)")
				.param("m", messageId).param("c", conversationId).query(Boolean.class).single();
	}

	public boolean hasAttachment(long conversationId, UUID fileId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM chat_message_attachment a JOIN chat_message m ON m.id = a.message_id
				WHERE a.file_id = :f AND m.conversation_id = :c AND m.deleted_at IS NULL)
				""").param("f", fileId).param("c", conversationId).query(Boolean.class).single();
	}

	public int unreadConversationCount(long userId) {
		return jdbc.sql("""
				SELECT count(*) FROM chat_member cm
				WHERE cm.user_id = :uid AND NOT cm.muted
				  AND EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = cm.conversation_id
				              AND m.id > cm.last_read_message_id AND m.sender_id <> :uid AND m.deleted_at IS NULL)
				""").param("uid", userId).query(Integer.class).single();
	}

	public String readOnlyReason(ConvRef ref, long userId) {
		if (ref.type() == ConversationType.DIRECT) {
			record Other(String first, String last, String status) {
			}
			Other o = jdbc.sql("""
					SELECT u.first_name AS first, u.last_name AS last, u.status
					FROM chat_member cm JOIN app_user u ON u.id = cm.user_id
					WHERE cm.conversation_id = :cid AND cm.user_id <> :uid
					""").param("cid", ref.id()).param("uid", userId).query(Other.class).optional().orElse(null);
			if (o != null && "PASSIVE".equals(o.status())) {
				return o.first() + " " + o.last() + " artık aktif personel değil; mesaj gönderilemez.";
			}
		}
		if (!ref.readOnly()) {
			return null;
		}
		return ref.type() == ConversationType.GROUP ? "Bu grup arşivlendi; yeni mesaj gönderilemez."
				: "Bu sohbet sona erdi.";
	}

	public List<ConversationRow> conversations(long userId) {
		return conversations(userId, null);
	}

	private List<ConversationRow> conversations(long userId, Long onlyId) {
		List<ListRow> rows = jdbc.sql("""
				SELECT c.id, c.type, c.title, c.read_only, c.last_message_at, cm.muted,
				       (SELECT count(*) FROM chat_member x WHERE x.conversation_id = c.id) AS member_count,
				       o.id AS other_id, o.first_name AS other_first, o.last_name AS other_last, o.status AS other_status,
				       lm.id AS lm_id, lm.content AS lm_content, lm.deleted_at AS lm_deleted, lm.sender_id AS lm_sender,
				       ls.first_name AS lm_sender_first,
				       (SELECT count(*) FROM chat_message m WHERE m.conversation_id = c.id AND m.id > cm.last_read_message_id
				          AND m.sender_id <> :uid AND m.deleted_at IS NULL) AS unread_count
				FROM chat_member cm
				JOIN chat_conversation c ON c.id = cm.conversation_id
				LEFT JOIN chat_member om ON c.type = 'DIRECT' AND om.conversation_id = c.id AND om.user_id <> :uid
				LEFT JOIN app_user o ON o.id = om.user_id
				LEFT JOIN chat_message lm ON lm.id = c.last_message_id
				LEFT JOIN app_user ls ON ls.id = lm.sender_id
				WHERE cm.user_id = :uid AND ((CAST(:cid AS bigint) IS NULL AND (c.type <> 'DIRECT' OR c.last_message_id IS NOT NULL))
				      OR c.id = CAST(:cid AS bigint))
				ORDER BY c.last_message_at DESC NULLS LAST, c.id DESC
				""").param("uid", userId).param("cid", onlyId).query(ListRow.class).list();

		List<Long> attachmentOnly = rows.stream()
				.filter(r -> r.lmId() != null && r.lmDeleted() == null && (r.lmContent() == null || r.lmContent().isBlank()))
				.map(ListRow::lmId).toList();
		Map<Long, List<String>> kinds = attachmentKinds(attachmentOnly);

		List<ConversationRow> result = new ArrayList<>();
		for (ListRow r : rows) {
			boolean direct = r.type() == ConversationType.DIRECT;
			boolean otherPassive = direct && "PASSIVE".equals(r.otherStatus());
			OtherRef other = direct ? new OtherRef(r.otherId(), r.otherFirst() + " " + r.otherLast(), otherPassive)
					: null;
			LastMessage last = null;
			if (r.lmId() != null) {
				boolean deleted = r.lmDeleted() != null;
				String snippet = deleted ? ""
						: ChatText.snippet(r.lmContent(), kinds.getOrDefault(r.lmId(), List.of()));
				last = new LastMessage(r.lmSender() != null && r.lmSender() == userId, r.lmSenderFirst(), snippet,
						r.lastMessageAt(), deleted);
			}
			result.add(new ConversationRow(r.id(), r.type(), direct ? other.fullName() : r.title(), other,
					(int) r.memberCount(), r.muted(), r.readOnly() || otherPassive, (int) r.unreadCount(), last));
		}
		return result;
	}

	private Map<Long, List<String>> attachmentKinds(Collection<Long> messageIds) {
		Map<Long, List<String>> kinds = new HashMap<>();
		if (messageIds.isEmpty()) {
			return kinds;
		}
		jdbc.sql("""
				SELECT a.message_id, f.content_type FROM chat_message_attachment a
				JOIN stored_file f ON f.id = a.file_id WHERE a.message_id IN (:ids) ORDER BY a.message_id, a.sort_order
				""").param("ids", messageIds).query(KindRow.class).list()
				.forEach(k -> kinds.computeIfAbsent(k.messageId(), x -> new ArrayList<>())
						.add(ChatText.kind(k.contentType())));
		return kinds;
	}

	public ConversationRow row(long userId, long conversationId) {
		List<ConversationRow> rows = conversations(userId, conversationId);
		if (rows.isEmpty()) {
			throw new BusinessException(ChatErrorCode.CHAT_CONVERSATION_NOT_FOUND);
		}
		return rows.get(0);
	}

	public ConversationDetail detail(UserContext me, long conversationId) {
		ConvRef ref = requireAccess(conversationId, me.userId());
		ConversationRow row = row(me.userId(), conversationId);
		List<ChatMemberDto> members = jdbc.sql("""
				SELECT u.id, u.first_name, u.last_name, u.status, sm.title, b.name AS branch_name, cm.role
				FROM chat_member cm JOIN app_user u ON u.id = cm.user_id
				LEFT JOIN season_membership sm ON sm.user_id = u.id AND sm.season_id = :sid
				LEFT JOIN branch b ON b.id = sm.branch_id
				WHERE cm.conversation_id = :cid
				ORDER BY (cm.role = 'OWNER') DESC, u.last_name COLLATE "tr-x-icu", u.first_name COLLATE "tr-x-icu"
				""").param("sid", me.seasonId()).param("cid", conversationId).query(MemberRow.class).list().stream()
				.map(m -> new ChatMemberDto(m.id(), m.firstName() + " " + m.lastName(), m.title(), m.branchName(),
						"PASSIVE".equals(m.status()), m.role()))
				.toList();
		String reason = readOnlyReason(ref, me.userId());
		Long otherRead = ref.type() == ConversationType.DIRECT
				? jdbc.sql("""
						SELECT last_read_message_id FROM chat_member WHERE conversation_id = :cid AND user_id <> :uid
						""").param("cid", conversationId).param("uid", me.userId()).query(Long.class).optional()
						.orElse(null)
				: null;
		boolean free = ref.type() == ConversationType.FREE;
		return new ConversationDetail(row.id(), row.type(), row.title(), row.other(), row.memberCount(), row.muted(),
				row.readOnly(), row.unreadCount(), row.lastMessage(), members, reason == null, reason,
				free && ref.role() == ChatRole.OWNER && !ref.readOnly(), free, otherRead);
	}

	public CursorResponse<MessageDto> messages(UserContext me, long conversationId, Long before, int size) {
		List<MsgRow> rows = jdbc.sql(MSG_SELECT + """
				WHERE m.conversation_id = :cid AND (CAST(:before AS bigint) IS NULL OR m.id < CAST(:before AS bigint))
				ORDER BY m.id DESC LIMIT :limit
				""").param("sid", me.seasonId()).param("cid", conversationId).param("before", before)
				.param("limit", size + 1).query(MsgRow.class).list();
		boolean more = rows.size() > size;
		List<MsgRow> page = more ? rows.subList(0, size) : rows;
		List<MessageDto> items = build(page, me.userId(), me.seasonId());
		return new CursorResponse<>(items, more ? items.get(items.size() - 1).id() : null);
	}

	public MessageDto message(long id, long viewerId, long activeSeasonId) {
		List<MsgRow> rows = jdbc.sql(MSG_SELECT + " WHERE m.id = :id")
				.param("sid", activeSeasonId).param("id", id).query(MsgRow.class).list();
		return build(rows, viewerId, activeSeasonId).get(0);
	}

	private static final String MSG_SELECT = """
			SELECT m.id, m.conversation_id, m.season_id, m.sender_id, m.content, m.reply_to_id, m.client_id,
			       m.created_at, m.deleted_at, u.first_name, u.last_name, u.status, sm.title, b.name AS branch_name
			FROM chat_message m JOIN app_user u ON u.id = m.sender_id
			LEFT JOIN season_membership sm ON sm.user_id = u.id AND sm.season_id = :sid
			LEFT JOIN branch b ON b.id = sm.branch_id
			""";

	private List<MessageDto> build(List<MsgRow> rows, long viewerId, long activeSeasonId) {
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> replyIds = rows.stream().map(MsgRow::replyToId).filter(x -> x != null).distinct().toList();
		List<Long> attachmentOwners = new ArrayList<>(rows.stream().filter(r -> r.deletedAt() == null)
				.map(MsgRow::id).toList());
		attachmentOwners.addAll(replyIds);

		Map<Long, List<AttRow>> attachments = new HashMap<>();
		if (!attachmentOwners.isEmpty()) {
			jdbc.sql("""
					SELECT a.message_id, f.id AS file_id, f.original_name, f.content_type, f.size_bytes
					FROM chat_message_attachment a JOIN stored_file f ON f.id = a.file_id
					WHERE a.message_id IN (:ids) ORDER BY a.message_id, a.sort_order
					""").param("ids", attachmentOwners).query(AttRow.class).list()
					.forEach(a -> attachments.computeIfAbsent(a.messageId(), x -> new ArrayList<>()).add(a));
		}
		Map<Long, ReplyRow> replies = new HashMap<>();
		if (!replyIds.isEmpty()) {
			jdbc.sql("""
					SELECT r.id, r.content, r.deleted_at, u.first_name, u.last_name
					FROM chat_message r JOIN app_user u ON u.id = r.sender_id WHERE r.id IN (:ids)
					""").param("ids", replyIds).query(ReplyRow.class).list().forEach(r -> replies.put(r.id(), r));
		}

		List<MessageDto> result = new ArrayList<>();
		for (MsgRow r : rows) {
			boolean deleted = r.deletedAt() != null;
			List<ChatAttachment> atts = deleted ? List.of()
					: attachments.getOrDefault(r.id(), List.of()).stream()
							.map(a -> new ChatAttachment(a.fileId().toString(), a.originalName(), a.contentType(),
									a.sizeBytes(), ChatText.kind(a.contentType())))
							.toList();
			ReplyRef reply = null;
			if (r.replyToId() != null) {
				ReplyRow rr = replies.get(r.replyToId());
				if (rr != null) {
					boolean rDeleted = rr.deletedAt() != null;
					List<String> kinds = attachments.getOrDefault(rr.id(), List.of()).stream()
							.map(a -> ChatText.kind(a.contentType())).toList();
					reply = new ReplyRef(rr.id(), rr.firstName() + " " + rr.lastName(),
							rDeleted ? "" : ChatText.snippet(rr.content(), kinds), rDeleted);
				}
			}
			boolean mine = r.senderId() == viewerId;
			boolean archived = r.seasonId() != activeSeasonId;
			ChatPerson sender = new ChatPerson(r.senderId(), r.firstName() + " " + r.lastName(), r.title(),
					r.branchName(), "PASSIVE".equals(r.status()));
			result.add(new MessageDto(r.id(), r.conversationId(), r.seasonId(), archived, r.clientId(), sender,
					deleted ? null : r.content(), atts, reply, r.createdAt(), deleted, mine,
					mine && !deleted && !archived));
		}
		return result;
	}

}
