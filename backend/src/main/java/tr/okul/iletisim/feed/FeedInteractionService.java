package tr.okul.iletisim.feed;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.feed.FeedDtos.CommentDto;
import tr.okul.iletisim.feed.FeedDtos.CommentRequest;
import tr.okul.iletisim.feed.FeedDtos.LikeResponse;
import tr.okul.iletisim.feed.FeedDtos.ReadStatusResponse;
import tr.okul.iletisim.feed.FeedDtos.RemindResponse;
import tr.okul.iletisim.feed.FeedQueries.CommentRef;
import tr.okul.iletisim.feed.FeedQueries.PostRef;

@Service
public class FeedInteractionService {

	private static final DateTimeFormatter REMIND_FORMAT = DateTimeFormatter.ofPattern("d MMMM HH:mm",
			Locale.forLanguageTag("tr"));
	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private final FeedQueries queries;
	private final FeedPostRepository posts;
	private final FeedCommentRepository comments;
	private final JdbcClient jdbc;
	private final NotificationService notifications;
	private final RealtimePublisher realtime;
	private final AuditService audit;

	public FeedInteractionService(FeedQueries queries, FeedPostRepository posts, FeedCommentRepository comments,
			JdbcClient jdbc, NotificationService notifications, RealtimePublisher realtime, AuditService audit) {
		this.queries = queries;
		this.posts = posts;
		this.comments = comments;
		this.jdbc = jdbc;
		this.notifications = notifications;
		this.realtime = realtime;
		this.audit = audit;
	}

	@Transactional
	public LikeResponse like(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		jdbc.sql("INSERT INTO feed_like (post_id, user_id) VALUES (:pid, :uid) ON CONFLICT DO NOTHING")
				.param("pid", id).param("uid", me.userId()).update();
		return new LikeResponse(true, queries.likeCount(id));
	}

	@Transactional
	public LikeResponse unlike(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		jdbc.sql("DELETE FROM feed_like WHERE post_id = :pid AND user_id = :uid")
				.param("pid", id).param("uid", me.userId()).update();
		return new LikeResponse(false, queries.likeCount(id));
	}

	@Transactional
	public void markRead(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		if (!ref.important()) {
			throw new BusinessException(FeedErrorCode.FEED_NOT_IMPORTANT);
		}
		if (ref.authorId() == me.userId()) {
			return;
		}
		jdbc.sql("INSERT INTO feed_read (post_id, user_id) VALUES (:pid, :uid) ON CONFLICT DO NOTHING")
				.param("pid", id).param("uid", me.userId()).update();
		realtime.toUsers(List.of(me.userId()), "BADGES", Map.of());
	}

	@Transactional(readOnly = true)
	public ReadStatusResponse reads(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		if (!ref.important()) {
			throw new BusinessException(FeedErrorCode.FEED_NOT_IMPORTANT);
		}
		if (ref.authorId() != me.userId()) {
			throw new BusinessException(FeedErrorCode.FEED_READS_FORBIDDEN);
		}
		var status = queries.readStatus(ref);
		return new ReadStatusResponse(status.get("read"), status.get("unread"), ref.lastReminderAt());
	}

	@Transactional
	public RemindResponse remind(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		if (ref.authorId() != me.userId()) {
			throw new BusinessException(FeedErrorCode.FEED_READS_FORBIDDEN);
		}
		me.requireWritable(ref.seasonId());
		if (!ref.important()) {
			throw new BusinessException(FeedErrorCode.FEED_NOT_IMPORTANT);
		}
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (ref.lastReminderAt() != null && ref.lastReminderAt().plus(Duration.ofHours(24)).isAfter(now)) {
			String when = ref.lastReminderAt().plus(Duration.ofHours(24)).atZoneSameInstant(ISTANBUL)
					.format(REMIND_FORMAT);
			throw new BusinessException(FeedErrorCode.FEED_REMIND_TOO_SOON, when);
		}
		Collection<Long> unread = queries.unreadRecipients(ref);
		if (unread.isEmpty()) {
			throw new BusinessException(FeedErrorCode.FEED_REMIND_NONE);
		}
		int claimed = jdbc.sql("""
				UPDATE feed_post SET last_reminder_at = now()
				WHERE id = :id AND (last_reminder_at IS NULL OR last_reminder_at < now() - interval '24 hours')
				""").param("id", id).update();
		if (claimed == 0) {
			OffsetDateTime last = jdbc.sql("SELECT last_reminder_at FROM feed_post WHERE id = :id").param("id", id)
					.query(OffsetDateTime.class).optional().orElse(null);
			String when = (last == null ? now : last).plus(Duration.ofHours(24)).atZoneSameInstant(ISTANBUL)
					.format(REMIND_FORMAT);
			throw new BusinessException(FeedErrorCode.FEED_REMIND_TOO_SOON, when);
		}
		notifications.notify(unread, "FEED_REMINDER", "Hatırlatma: Okumanız gereken bir önemli duyuru var",
				FeedText.snippet(ref.content(), 120), "/akis/" + id);
		return new RemindResponse(unread.size());
	}

	@Transactional
	public CommentDto addComment(UserContext me, long id, CommentRequest request) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		String content = FeedText.trimToNull(request.content());
		if (content == null || content.length() > 1000) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "content");
		}
		FeedComment c = new FeedComment();
		c.setPostId(id);
		c.setAuthorId(me.userId());
		c.setContent(content);
		c = comments.saveAndFlush(c);
		if (ref.authorId() != me.userId()) {
			notifications.notify(List.of(ref.authorId()), "FEED_COMMENT",
					me.fullName() + " paylaşımınıza yorum yaptı", FeedText.snippet(content, 120), "/akis/" + id);
		}
		return queries.comment(me, c.getId());
	}

	@Transactional
	public void deleteComment(UserContext me, long commentId) {
		me.requireMember();
		CommentRef ref = queries.findVisibleComment(commentId, me.userId());
		me.requireWritable(ref.seasonId());
		boolean own = ref.authorId() == me.userId();
		if (!own && ref.postAuthorId() != me.userId() && !me.isManager()) {
			throw new BusinessException(FeedErrorCode.FEED_COMMENT_DELETE_FORBIDDEN);
		}
		jdbc.sql("UPDATE feed_comment SET deleted_at = now(), deleted_by = :uid WHERE id = :id")
				.param("uid", me.userId()).param("id", commentId).update();
		if (!own) {
			audit.log(me.userId(), FeedAuditAction.FEED_COMMENT_DELETED, "FEED_COMMENT", commentId,
					Map.of("authorId", ref.authorId(), "postId", ref.postId()));
		}
	}

}
