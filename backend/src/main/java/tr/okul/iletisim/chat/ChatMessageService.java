package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tr.okul.iletisim.chat.ChatDtos.MessageDto;
import tr.okul.iletisim.chat.ChatDtos.SendMessageRequest;
import tr.okul.iletisim.chat.ChatDtos.SendResult;
import tr.okul.iletisim.chat.ChatQueries.ConvRef;
import tr.okul.iletisim.chat.ChatQueries.MemberFlag;
import tr.okul.iletisim.chat.ChatQueries.MessageRef;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;

@Service
public class ChatMessageService {

	private final ChatMessageRepository messages;
	private final ChatMessageAttachmentRepository attachmentRepository;
	private final ChatQueries queries;
	private final FileStorageService files;
	private final StoredFileRepository fileRepository;
	private final NotificationService notifications;
	private final RealtimePublisher realtime;
	private final JdbcClient jdbc;
	private final TransactionTemplate tx;

	public ChatMessageService(ChatMessageRepository messages, ChatMessageAttachmentRepository attachmentRepository,
			ChatQueries queries, FileStorageService files, StoredFileRepository fileRepository,
			NotificationService notifications, RealtimePublisher realtime, JdbcClient jdbc,
			PlatformTransactionManager txManager) {
		this.messages = messages;
		this.attachmentRepository = attachmentRepository;
		this.queries = queries;
		this.files = files;
		this.fileRepository = fileRepository;
		this.notifications = notifications;
		this.realtime = realtime;
		this.jdbc = jdbc;
		this.tx = new TransactionTemplate(txManager);
	}

	@Transactional(readOnly = true)
	public CursorResponse<MessageDto> list(UserContext me, long conversationId, Long before, int size) {
		me.requireMember();
		queries.requireAccess(conversationId, me.userId());
		return queries.messages(me, conversationId, before, Math.max(1, Math.min(size, 100)));
	}

	@Transactional(readOnly = true)
	public StoredFile file(UserContext me, long conversationId, UUID fileId) {
		me.requireMember();
		queries.requireAccess(conversationId, me.userId());
		if (!queries.hasAttachment(conversationId, fileId)) {
			throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
		}
		return files.get(fileId);
	}

	public SendResult send(UserContext me, long conversationId, SendMessageRequest request) {
		try {
			return tx.execute(status -> doSend(me, conversationId, request));
		}
		catch (DataIntegrityViolationException e) {
			if (!rootMessage(e).contains("ux_chat_message_client")) {
				throw e;
			}
			long seasonId = me.requireMember();
			SendResult existing = tx.execute(status -> messages
					.findByConversationIdAndSenderIdAndClientId(conversationId, me.userId(), request.clientId())
					.map(m -> new SendResult(queries.message(m.getId(), me.userId(), seasonId), false)).orElse(null));
			if (existing == null) {
				throw e;
			}
			return existing;
		}
	}

	private static String rootMessage(Throwable ex) {
		StringBuilder sb = new StringBuilder();
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t.getMessage() != null) {
				sb.append(t.getMessage()).append(' ');
			}
		}
		return sb.toString();
	}

	private SendResult doSend(UserContext me, long conversationId, SendMessageRequest request) {
		long seasonId = me.requireMember();
		ConvRef ref = queries.requireAccess(conversationId, me.userId());
		ChatMessage existing = messages
				.findByConversationIdAndSenderIdAndClientId(conversationId, me.userId(), request.clientId()).orElse(null);
		if (existing != null) {
			return new SendResult(queries.message(existing.getId(), me.userId(), seasonId), false);
		}
		String reason = queries.readOnlyReason(ref, me.userId());
		if (reason != null) {
			throw new BusinessException(ChatErrorCode.CHAT_READ_ONLY, reason);
		}
		Set<UUID> fileIds = new LinkedHashSet<>(request.fileIds());
		if (fileIds.size() > 10) {
			throw new BusinessException(ChatErrorCode.CHAT_TOO_MANY_FILES);
		}
		if (request.content() == null && fileIds.isEmpty()) {
			throw new BusinessException(ChatErrorCode.CHAT_EMPTY_MESSAGE);
		}
		if (request.replyToId() != null) {
			MessageRef reply = queries.messageRef(request.replyToId());
			if (reply == null || reply.conversationId() != conversationId || reply.deletedAt() != null) {
				throw new BusinessException(ChatErrorCode.CHAT_REPLY_INVALID);
			}
		}

		ChatMessage m = new ChatMessage();
		m.setConversationId(conversationId);
		m.setSeasonId(seasonId);
		m.setSenderId(me.userId());
		m.setContent(request.content());
		m.setReplyToId(request.replyToId());
		m.setClientId(request.clientId());
		m = messages.saveAndFlush(m);
		long messageId = m.getId();

		List<ChatMessageAttachment> rows = new ArrayList<>();
		List<String> kinds = new ArrayList<>();
		short order = 0;
		for (UUID fileId : fileIds) {
			StoredFile f = files.attach(fileId, me, FilePurpose.CHAT);
			rows.add(new ChatMessageAttachment(messageId, fileId, order++));
			kinds.add(ChatText.kind(f.getContentType()));
		}
		attachmentRepository.saveAllAndFlush(rows);

		jdbc.sql("""
				UPDATE chat_conversation SET last_message_id = :mid, last_message_at = now()
				WHERE id = :cid AND (last_message_id IS NULL OR last_message_id < :mid)
				""")
				.param("mid", messageId).param("cid", conversationId).update();

		MessageDto dto = queries.message(messageId, me.userId(), seasonId);
		List<MemberFlag> flags = queries.memberFlags(conversationId);
		List<Long> others = flags.stream().map(MemberFlag::userId).filter(u -> u != me.userId()).toList();
		realtime.toUsers(List.of(me.userId()), "CHAT_MESSAGE",
				Map.of("conversationId", conversationId, "message", dto));
		realtime.toUsers(others, "CHAT_MESSAGE",
				Map.of("conversationId", conversationId, "message", dto.forViewer(false, false)));
		realtime.toUsers(others, "BADGES", Map.of());

		List<Long> pushRecipients = flags.stream().filter(f -> f.userId() != me.userId() && !f.muted())
				.map(MemberFlag::userId).toList();
		String snippet = ChatText.snippet(request.content(), kinds);
		boolean direct = ref.type() == ConversationType.DIRECT;
		notifications.pushOnly(pushRecipients, direct ? me.fullName() : ref.title(),
				direct ? snippet : me.firstName() + ": " + snippet, "/mesajlar/" + conversationId,
				"chat-" + conversationId);
		return new SendResult(dto, true);
	}

	@Transactional
	public void delete(UserContext me, long messageId) {
		me.requireMember();
		MessageRef ref = queries.messageRef(messageId);
		if (ref == null || !queries.memberIds(ref.conversationId()).contains(me.userId())) {
			throw new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);
		}
		if (ref.senderId() != me.userId()) {
			throw new BusinessException(ChatErrorCode.CHAT_DELETE_FORBIDDEN);
		}
		if (ref.deletedAt() != null) {
			return;
		}
		me.requireWritable(ref.seasonId());
		ChatMessage m = messages.findById(messageId)
				.orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));
		m.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
		m.setContent(null);
		messages.saveAndFlush(m);
		for (ChatMessageAttachment a : attachmentRepository.findByMessageId(messageId)) {
			fileRepository.findById(a.getFileId()).filter(f -> f.getDeletedAt() == null).ifPresent(files::delete);
		}
		attachmentRepository.deleteByMessageId(messageId);
		attachmentRepository.flush();
		List<Long> members = queries.memberIds(ref.conversationId());
		realtime.toUsers(members, "CHAT_MESSAGE_DELETED",
				Map.of("conversationId", ref.conversationId(), "messageId", messageId));
		realtime.toUsers(members.stream().filter(u -> u != me.userId()).toList(), "BADGES", Map.of());
	}

	@Transactional
	public void markRead(UserContext me, long conversationId, long lastMessageId) {
		me.requireMember();
		ConvRef ref = queries.requireAccess(conversationId, me.userId());
		if (!queries.messageInConversation(lastMessageId, conversationId)) {
			return;
		}
		int updated = jdbc.sql("""
				UPDATE chat_member SET last_read_message_id = GREATEST(last_read_message_id, :m)
				WHERE conversation_id = :c AND user_id = :u AND last_read_message_id < :m
				""").param("m", lastMessageId).param("c", conversationId).param("u", me.userId()).update();
		if (updated == 0) {
			return;
		}
		if (ref.type() == ConversationType.DIRECT) {
			List<Long> others = queries.memberIds(conversationId).stream().filter(u -> u != me.userId()).toList();
			realtime.toUsers(others, "CHAT_READ",
					Map.of("conversationId", conversationId, "lastReadMessageId", lastMessageId));
		}
		realtime.toUsers(List.of(me.userId()), "BADGES", Map.of());
	}

}
