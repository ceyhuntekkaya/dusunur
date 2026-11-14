package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;

public final class FeedDtos {

	private static final String COMMENT_MESSAGE = "Yorum 1–1000 karakter olmalıdır.";
	private static final String CONTENT_MESSAGE = "Metin en fazla 5000 karakter olabilir.";

	private FeedDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FeedAuthor(long id, String fullName, String title, String branchName, boolean former) {
	}

	public record FeedAttachment(String fileId, String name, String contentType, long sizeBytes, String kind) {
	}

	public record CommentDto(long id, long postId, FeedAuthor author, String content, OffsetDateTime createdAt,
			boolean canDelete) {
	}

	public record ReadStats(int read, int total) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PostDto(long id, long seasonId, boolean archived, FeedAuthor author, String content,
			boolean important, boolean pinned, OffsetDateTime editedAt, OffsetDateTime createdAt,
			List<TargetView> targets, List<FeedAttachment> attachments, int likeCount, boolean liked,
			int commentCount, List<CommentDto> latestComments, Boolean readByMe, ReadStats readStats,
			boolean canEdit, boolean canDelete, boolean canPin) {
	}

	public record CreatePostRequest(
			@NotEmpty(message = "En az bir alıcı seçin.") List<TargetRef> targets,
			@Size(max = 5000, message = CONTENT_MESSAGE) String content,
			List<UUID> fileIds,
			boolean important) {
	}

	public record UpdatePostRequest(@Size(max = 5000, message = CONTENT_MESSAGE) String content) {
	}

	public record CreatePostResponse(PostDto post, int recipientCount) {
	}

	public record CommentRequest(
			@NotBlank(message = COMMENT_MESSAGE) @Size(max = 1000, message = COMMENT_MESSAGE) String content) {
	}

	public record FeedSeason(long id, String name, boolean active) {
	}

	public record LikeResponse(boolean liked, int likeCount) {
	}

	public record RemindResponse(int notified) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ReadUser(long userId, String fullName, String title, String branchName, OffsetDateTime readAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ReadStatusResponse(List<ReadUser> read, List<ReadUser> unread, OffsetDateTime lastReminderAt) {
	}

}
