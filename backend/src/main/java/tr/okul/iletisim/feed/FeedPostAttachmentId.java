package tr.okul.iletisim.feed;

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
public class FeedPostAttachmentId implements Serializable {

	private Long postId;
	private UUID fileId;

}
