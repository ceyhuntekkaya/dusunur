package tr.okul.iletisim.chat;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ChatMemberId implements Serializable {

	private Long conversationId;
	private Long userId;

}
