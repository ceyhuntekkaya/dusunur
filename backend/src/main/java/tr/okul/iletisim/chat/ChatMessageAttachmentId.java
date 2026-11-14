package tr.okul.iletisim.chat;

import java.io.Serializable;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ChatMessageAttachmentId implements Serializable {

	private Long messageId;
	private UUID fileId;

}
