package tr.okul.iletisim.feed;

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
import tr.okul.iletisim.core.directory.TargetType;

@Entity
@Table(name = "feed_post_target")
@Getter
@Setter
public class FeedPostTarget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id")
	private Long postId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type")
	private TargetType targetType;

	@Column(name = "target_id")
	private Long targetId;

}
