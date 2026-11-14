package tr.okul.iletisim.dms;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@Table(name = "dms_file")
@Getter
@Setter
public class DmsFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "space_id")
	private Long spaceId;

	@Column(name = "folder_id")
	private Long folderId;

	private String name;

	@Column(name = "name_key")
	private String nameKey;

	@Column(name = "search_key")
	private String searchKey;

	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "size_bytes")
	private long sizeBytes;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "uploaded_by")
	private Long uploadedBy;

	@Column(name = "updated_by")
	private Long updatedBy;

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
