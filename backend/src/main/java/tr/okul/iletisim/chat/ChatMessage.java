package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "conversation_id")
	private Long conversationId;

	@Column(name = "season_id")
	private Long seasonId;

	@Column(name = "sender_id")
	private Long senderId;

	private String content;

	@Column(name = "reply_to_id")
	private Long replyToId;

	@Column(name = "client_id")
	private UUID clientId;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

}
