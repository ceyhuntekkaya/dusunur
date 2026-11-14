package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;

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
@Table(name = "feed_comment")
@Getter
@Setter
public class FeedComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id")
	private Long postId;

	@Column(name = "author_id")
	private Long authorId;

	private String content;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "deleted_by")
	private Long deletedBy;

}
