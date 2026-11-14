package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_member")
@IdClass(ChatMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatMember {

	@Id
	@Column(name = "conversation_id")
	private Long conversationId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	private ChatRole role = ChatRole.MEMBER;

	private boolean muted;

	@Column(name = "last_read_message_id")
	private long lastReadMessageId;

	@CreationTimestamp
	@Column(name = "joined_at", updatable = false)
	private OffsetDateTime joinedAt;

	public ChatMember(Long conversationId, Long userId, ChatRole role, long lastReadMessageId) {
		this.conversationId = conversationId;
		this.userId = userId;
		this.role = role;
		this.lastReadMessageId = lastReadMessageId;
	}

}
