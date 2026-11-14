package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feed_post")
@Getter
@Setter
public class FeedPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "season_id")
	private Long seasonId;

	@Column(name = "author_id")
	private Long authorId;

	private String content;

	private boolean important;

	@Column(name = "pinned_at")
	private OffsetDateTime pinnedAt;

	@Column(name = "pinned_by")
	private Long pinnedBy;

	@Column(name = "edited_at")
	private OffsetDateTime editedAt;

	@Column(name = "last_reminder_at")
	private OffsetDateTime lastReminderAt;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "deleted_by")
	private Long deletedBy;

}
