package tr.okul.iletisim.chat;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_message_attachment")
@IdClass(ChatMessageAttachmentId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageAttachment {

	@Id
	@Column(name = "message_id")
	private Long messageId;

	@Id
	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "sort_order")
	private short sortOrder;

	public ChatMessageAttachment(Long messageId, UUID fileId, short sortOrder) {
		this.messageId = messageId;
		this.fileId = fileId;
		this.sortOrder = sortOrder;
	}

}
