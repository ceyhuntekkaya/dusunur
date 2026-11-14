package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.common.util.TextNormalizer;

public final class ChatDtos {

	private static final String TITLE_MESSAGE = "Sohbet adı 2–100 karakter olmalıdır.";
	private static final String CLIENT_ID_MESSAGE = "Mesaj gönderilemedi. Sayfayı yenileyip tekrar deneyin.";

	private ChatDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatPerson(long id, String fullName, String title, String branchName, boolean former) {
	}

	public record ChatAttachment(String fileId, String name, String contentType, long sizeBytes, String kind) {
	}

	public record ReplyRef(long id, String senderName, String snippet, boolean deleted) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record MessageDto(long id, long conversationId, long seasonId, boolean archived, UUID clientId,
			ChatPerson sender, String content, List<ChatAttachment> attachments, ReplyRef replyTo,
			OffsetDateTime createdAt, boolean deleted, boolean mine, boolean canDelete) {

		public MessageDto forViewer(boolean mine, boolean canDelete) {
			return new MessageDto(id, conversationId, seasonId, archived, clientId, sender, content, attachments,
					replyTo, createdAt, deleted, mine, canDelete);
		}

	}

	public record OtherRef(long id, String fullName, boolean former) {
	}

	public record LastMessage(boolean mine, String senderFirstName, String snippet, OffsetDateTime createdAt,
			boolean deleted) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ConversationRow(long id, ConversationType type, String title, OtherRef other, int memberCount,
			boolean muted, boolean readOnly, int unreadCount, LastMessage lastMessage) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatMemberDto(long id, String fullName, String title, String branchName, boolean former,
			ChatRole role) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ConversationDetail(long id, ConversationType type, String title, OtherRef other, int memberCount,
			boolean muted, boolean readOnly, int unreadCount, LastMessage lastMessage, List<ChatMemberDto> members,
			boolean canSend, String readOnlyReason, boolean canManage, boolean canLeave,
			Long otherLastReadMessageId) {
	}

	public record SendMessageRequest(
			@NotNull(message = CLIENT_ID_MESSAGE) UUID clientId,
			@Size(max = 4000, message = "Mesaj en fazla 4000 karakter olabilir.") String content,
			List<UUID> fileIds,
			Long replyToId) {

		public SendMessageRequest {
			content = content == null || content.isBlank() ? null : content.trim();
			fileIds = fileIds == null ? List.of() : fileIds;
		}

	}

	public record DirectRequest(@NotNull(message = "Bir kişi seçin.") Long userId) {
	}

	public record CreateGroupRequest(
			@NotNull(message = TITLE_MESSAGE) @Size(min = 2, max = 100, message = TITLE_MESSAGE) String title,
			List<Long> memberIds) {

		public CreateGroupRequest {
			title = TextNormalizer.clean(title);
			memberIds = memberIds == null ? List.of() : memberIds;
		}

	}

	public record RenameRequest(
			@NotNull(message = TITLE_MESSAGE) @Size(min = 2, max = 100, message = TITLE_MESSAGE) String title) {

		public RenameRequest {
			title = TextNormalizer.clean(title);
		}

	}

	public record AddMembersRequest(
			@NotEmpty(message = "En az 1, en fazla 100 kişi seçin.") @Size(max = 100, message = "En az 1, en fazla 100 kişi seçin.") List<Long> userIds) {
	}

	public record MuteRequest(boolean muted) {
	}

	public record ReadRequest(@NotNull(message = "Okunan mesaj bilgisi gönderilmedi.") Long lastMessageId) {
	}

	public record SendResult(MessageDto message, boolean created) {
	}

}
