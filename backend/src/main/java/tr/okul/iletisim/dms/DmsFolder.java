package tr.okul.iletisim.dms;

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
@Table(name = "dms_folder")
@Getter
@Setter
public class DmsFolder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "space_id")
	private Long spaceId;

	@Column(name = "parent_id")
	private Long parentId;

	private String name;

	@Column(name = "name_key")
	private String nameKey;

	@Column(name = "search_key")
	private String searchKey;

	private short depth;

	@Column(name = "created_by")
	private Long createdBy;

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

	@Column(name = "trash_root")
	private boolean trashRoot;

	@Column(name = "trashed_with")
	private Long trashedWith;

}
