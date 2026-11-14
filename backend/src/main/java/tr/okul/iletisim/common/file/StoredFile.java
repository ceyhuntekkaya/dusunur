package tr.okul.iletisim.common.file;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stored_file")
@Getter
@Setter
public class StoredFile {

	@Id
	private UUID id;

	@Column(name = "original_name")
	private String originalName;

	@Column(name = "content_type")
	private String contentType;

	@Column(name = "size_bytes")
	private long sizeBytes;

	@Column(name = "storage_path")
	private String storagePath;

	@Enumerated(EnumType.STRING)
	private FilePurpose purpose;

	@Column(name = "uploaded_by")
	private Long uploadedBy;

	private boolean attached;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

}
