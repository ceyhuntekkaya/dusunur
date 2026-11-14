package tr.okul.iletisim.dms;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;

public final class DmsDtos {

	private static final String NAME_MESSAGE = "Alan adı 2–100 karakter olmalıdır.";
	private static final String DESCRIPTION_MESSAGE = "Açıklama en fazla 500 karakter olabilir.";
	private static final String LEVEL_MESSAGE = "Bir yetki seviyesi seçin.";

	private DmsDtos() {
	}

	public enum OnConflict {
		ASK, OVERWRITE, RENAME
	}

	public record DmsPerson(long id, String fullName) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SpaceRow(long id, String name, String description, DmsPerson owner, DmsLevel myLevel, long fileCount,
			long totalBytes, OffsetDateTime lastUpdate) {
	}

	public record GrantRow(long id, TargetView subject, DmsLevel level) {
	}

	public record FolderRow(long id, String name, OffsetDateTime updatedAt, DmsPerson createdBy, int itemCount) {
	}

	public record FileRow(long id, String name, long sizeBytes, String contentType, boolean previewable,
			OffsetDateTime updatedAt, DmsPerson updatedBy, DmsPerson uploadedBy, OffsetDateTime createdAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FolderInfo(long id, String name, Long parentId) {
	}

	public record Crumb(long id, String name) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FolderContent(SpaceRow space, FolderInfo folder, List<Crumb> breadcrumb, List<FolderRow> folders,
			List<FileRow> files) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FolderNode(long id, Long parentId, String name, int depth) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record TrashRow(String type, long id, String name, String originalPath, DmsPerson deletedBy,
			OffsetDateTime deletedAt, OffsetDateTime purgeAt, Long sizeBytes) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SearchResult(String type, long id, String name, long spaceId, String spaceName, Long folderId,
			String path, Long sizeBytes, String contentType) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RestoreResult(boolean movedToRoot, String renamedTo) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record LocateResponse(long spaceId, Long folderId) {
	}

	public record PurgeResponse(int purged) {
	}

	public record GrantResult(GrantRow row, boolean created) {
	}

	public record FileSaveResult(FileRow row, boolean created) {
	}

	public record SpaceRequest(
			@NotNull(message = NAME_MESSAGE) @Size(min = 2, max = 100, message = NAME_MESSAGE) String name,
			@Size(max = 500, message = DESCRIPTION_MESSAGE) String description) {
	}

	public record GrantCreateRequest(@NotNull(message = "Bir kişi, grup veya şube seçin.") @Valid TargetRef subject,
			@NotNull(message = LEVEL_MESSAGE) DmsLevel level) {
	}

	public record GrantUpdateRequest(@NotNull(message = LEVEL_MESSAGE) DmsLevel level) {
	}

	public record FolderCreateRequest(Long parentId, String name) {
	}

	public record RenameRequest(String name) {
	}

	public record MoveRequest(Long targetFolderId) {
	}

	public record SaveFileRequest(Long folderId, @NotNull(message = "Bir dosya seçin.") UUID fileId,
			OnConflict onConflict) {
	}

}
