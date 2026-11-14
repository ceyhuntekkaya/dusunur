package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "feed_like")
@IdClass(FeedLikeId.class)
@Getter
@Setter
@NoArgsConstructor
public class FeedLike {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

}
