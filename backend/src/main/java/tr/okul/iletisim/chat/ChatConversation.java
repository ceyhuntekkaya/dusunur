package tr.okul.iletisim.chat;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
public class ChatConversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private ConversationType type;

	private String title;

	@Column(name = "direct_key")
	private String directKey;

	@Column(name = "staff_group_id")
	private Long staffGroupId;

	@Column(name = "read_only")
	private boolean readOnly;

	@Column(name = "last_message_id")
	private Long lastMessageId;

	@Column(name = "last_message_at")
	private OffsetDateTime lastMessageAt;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

}
