package tr.okul.iletisim.common.file;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

	java.util.List<StoredFile> findByAttachedFalseAndDeletedAtIsNullAndCreatedAtBefore(java.time.OffsetDateTime before);

}
