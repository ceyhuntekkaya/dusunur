package tr.okul.iletisim.feed;

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
@Table(name = "feed_post_attachment")
@IdClass(FeedPostAttachmentId.class)
@Getter
@Setter
@NoArgsConstructor
public class FeedPostAttachment {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Id
	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "sort_order")
	private short sortOrder;

	public FeedPostAttachment(Long postId, UUID fileId, short sortOrder) {
		this.postId = postId;
		this.fileId = fileId;
		this.sortOrder = sortOrder;
	}

}
