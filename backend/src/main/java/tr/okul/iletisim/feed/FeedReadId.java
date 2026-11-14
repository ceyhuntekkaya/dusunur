package tr.okul.iletisim.feed;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FeedReadId implements Serializable {

	private Long postId;
	private Long userId;

}
