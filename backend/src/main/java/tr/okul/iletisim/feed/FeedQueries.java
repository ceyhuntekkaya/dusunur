package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;
import tr.okul.iletisim.core.directory.AudienceResolver;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.feed.FeedDtos.CommentDto;
import tr.okul.iletisim.feed.FeedDtos.FeedAttachment;
import tr.okul.iletisim.feed.FeedDtos.FeedAuthor;
import tr.okul.iletisim.feed.FeedDtos.FeedSeason;
import tr.okul.iletisim.feed.FeedDtos.PostDto;
import tr.okul.iletisim.feed.FeedDtos.ReadStats;
import tr.okul.iletisim.feed.FeedDtos.ReadUser;

@Component
public class FeedQueries {

	public record PostRef(long id, long seasonId, long authorId, String content, boolean important,
			OffsetDateTime pinnedAt, OffsetDateTime lastReminderAt) {
	}

	public record CommentRef(long id, long postId, long authorId, long postAuthorId, long seasonId) {
	}

	private record PostRow(long id, long seasonId, long authorId, String content, boolean important,
			OffsetDateTime pinnedAt, OffsetDateTime editedAt, OffsetDateTime createdAt, String firstName,
			String lastName, String authorStatus, String authorTitle, String authorBranch) {
	}

	private record TargetRow(long postId, String targetType, Long targetId, String name) {
	}

	private record AttachmentRow(long postId, UUID fileId, String originalName, String contentType, long sizeBytes) {
	}

	private record LikeRow(long postId, int cnt, boolean liked) {
	}

	private record CountRow(long postId, int cnt) {
	}

	private record CommentRow(long id, long postId, long authorId, String content, OffsetDateTime createdAt,
			String firstName, String lastName, String authorStatus, String title, String branchName,
			long postAuthorId, long postSeasonId) {
	}

	private record UserRow(long userId, String fullName, String title, String branchName, OffsetDateTime readAt) {
	}

	private static final String COMMENT_SELECT = """
			SELECT c.id, c.post_id, c.author_id, c.content, c.created_at, u.first_name, u.last_name,
			       u.status AS author_status, sm.title, b.name AS branch_name,
			       p.author_id AS post_author_id, p.season_id AS post_season_id
			FROM feed_comment c
			JOIN feed_post p ON p.id = c.post_id
			JOIN app_user u ON u.id = c.author_id
			LEFT JOIN season_membership sm ON sm.season_id = p.season_id AND sm.user_id = c.author_id
			LEFT JOIN branch b ON b.id = sm.branch_id
			""";

	private final JdbcClient jdbc;
	private final AudienceResolver audience;

	public FeedQueries(JdbcClient jdbc, AudienceResolver audience) {
		this.jdbc = jdbc;
		this.audience = audience;
	}

	public PostRef assertVisible(long postId, long uid) {
		return jdbc.sql("""
				SELECT p.id, p.season_id, p.author_id, p.content, p.important, p.pinned_at, p.last_reminder_at
				FROM feed_post p
				WHERE p.id = :id AND p.deleted_at IS NULL AND
				""" + FeedVisibility.PREDICATE)
				.param("id", postId).param("uid", uid).query(PostRef.class).optional()
				.orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_POST_NOT_FOUND));
	}

	public CommentRef findVisibleComment(long commentId, long uid) {
		return jdbc.sql("""
				SELECT c.id, c.post_id, c.author_id, p.author_id AS post_author_id, p.season_id
				FROM feed_comment c JOIN feed_post p ON p.id = c.post_id
				WHERE c.id = :id AND c.deleted_at IS NULL AND p.deleted_at IS NULL AND
				""" + FeedVisibility.PREDICATE)
				.param("id", commentId).param("uid", uid).query(CommentRef.class).optional()
				.orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_COMMENT_NOT_FOUND));
	}

	public List<FeedSeason> seasons(long uid) {
		return jdbc.sql("""
				SELECT s.id, s.name, (s.status = 'ACTIVE') AS active
				FROM season s JOIN season_membership m ON m.season_id = s.id AND m.user_id = :uid
				WHERE s.status <> 'DRAFT'
				ORDER BY s.name DESC
				""").param("uid", uid).query(FeedSeason.class).list();
	}

	public CursorResponse<PostDto> page(UserContext me, Long seasonId, String filter, Long groupId, Long before,
			int size) {
		long season = seasonId == null ? me.requireMember() : seasonId;
		boolean assigned = jdbc.sql("SELECT EXISTS (SELECT 1 FROM season_membership WHERE season_id = :sid AND user_id = :uid)")
				.param("sid", season).param("uid", me.userId()).query(Boolean.class).single();
		if (!assigned) {
			return new CursorResponse<>(List.of(), null);
		}
		boolean active = me.seasonId() != null && me.seasonId() == season;
		String cond = switch (filter == null || filter.isBlank() ? "all" : filter) {
			case "all" -> active ? " AND p.pinned_at IS NULL" : "";
			case "important" -> " AND p.important";
			case "unreadImportant" -> " AND p.important AND p.author_id <> :uid AND NOT EXISTS (SELECT 1 FROM feed_read r WHERE r.post_id = p.id AND r.user_id = :uid)";
			case "direct" -> " AND EXISTS (SELECT 1 FROM feed_post_target t2 WHERE t2.post_id = p.id AND t2.target_type = 'USER' AND t2.target_id = :uid)";
			case "mine" -> " AND p.author_id = :uid";
			case "group" -> {
				if (groupId == null) {
					throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, "groupId");
				}
				yield " AND EXISTS (SELECT 1 FROM feed_post_target t3 WHERE t3.post_id = p.id AND t3.target_type = 'GROUP' AND t3.target_id = :groupId)";
			}
			default -> throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, "filter");
		};
		JdbcClient.StatementSpec spec = jdbc.sql("""
				SELECT p.id FROM feed_post p
				WHERE p.deleted_at IS NULL AND p.season_id = :seasonId AND
				""" + FeedVisibility.PREDICATE + cond + """
				 AND (CAST(:before AS bigint) IS NULL OR p.id < :before)
				ORDER BY p.id DESC
				LIMIT :limit
				""")
				.param("seasonId", season).param("uid", me.userId()).param("before", before)
				.param("limit", size + 1);
		if (groupId != null && cond.contains(":groupId")) {
			spec = spec.param("groupId", groupId);
		}
		List<Long> ids = spec.query(Long.class).list();
		Long next = null;
		if (ids.size() > size) {
			ids = ids.subList(0, size);
			next = ids.get(ids.size() - 1);
		}
		return new CursorResponse<>(loadCards(ids, me), next);
	}

	public List<PostDto> pinned(UserContext me) {
		long season = me.requireMember();
		List<Long> ids = jdbc.sql("""
				SELECT p.id FROM feed_post p
				WHERE p.deleted_at IS NULL AND p.season_id = :sid AND p.pinned_at IS NOT NULL AND
				""" + FeedVisibility.PREDICATE + """
				 ORDER BY p.pinned_at DESC LIMIT 3
				""").param("sid", season).param("uid", me.userId()).query(Long.class).list();
		return loadCards(ids, me);
	}

	public int unreadImportantCount(long seasonId, long uid) {
		return jdbc.sql("""
				SELECT count(*) FROM feed_post p
				WHERE p.deleted_at IS NULL AND p.season_id = :sid AND p.important AND p.author_id <> :uid
				  AND NOT EXISTS (SELECT 1 FROM feed_read r WHERE r.post_id = p.id AND r.user_id = :uid)
				  AND
				""" + FeedVisibility.PREDICATE).param("sid", seasonId).param("uid", uid)
				.query(Integer.class).single();
	}

	public List<TargetRef> targetsOf(long postId) {
		return jdbc.sql("SELECT target_type, target_id FROM feed_post_target WHERE post_id = :id ORDER BY id")
				.param("id", postId)
				.query((rs, i) -> new TargetRef(TargetType.valueOf(rs.getString("target_type")),
						(Long) rs.getObject("target_id")))
				.list();
	}

	public List<UUID> attachmentFileIds(long postId) {
		return jdbc.sql("SELECT file_id FROM feed_post_attachment WHERE post_id = :id ORDER BY sort_order")
				.param("id", postId).query(UUID.class).list();
	}

	public boolean hasAttachment(long postId, UUID fileId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM feed_post_attachment WHERE post_id = :id AND file_id = :fid)")
				.param("id", postId).param("fid", fileId).query(Boolean.class).single();
	}

	public int likeCount(long postId) {
		return jdbc.sql("SELECT count(*) FROM feed_like WHERE post_id = :id").param("id", postId)
				.query(Integer.class).single();
	}

	public CursorResponse<CommentDto> comments(UserContext me, long postId, Long after, int size) {
		List<CommentRow> rows = jdbc.sql(COMMENT_SELECT + """
				WHERE c.post_id = :pid AND c.deleted_at IS NULL AND (CAST(:after AS bigint) IS NULL OR c.id > :after)
				ORDER BY c.id ASC LIMIT :limit
				""").param("pid", postId).param("after", after).param("limit", size + 1)
				.query(CommentRow.class).list();
		Long next = null;
		if (rows.size() > size) {
			rows = rows.subList(0, size);
			next = rows.get(rows.size() - 1).id();
		}
		return new CursorResponse<>(rows.stream().map(r -> toComment(r, me)).toList(), next);
	}

	public CommentDto comment(UserContext me, long commentId) {
		return toComment(jdbc.sql(COMMENT_SELECT + " WHERE c.id = :id").param("id", commentId)
				.query(CommentRow.class).single(), me);
	}

	public List<PostDto> loadCards(List<Long> ids, UserContext me) {
		if (ids.isEmpty()) {
			return List.of();
		}
		long uid = me.userId();
		Map<Long, PostRow> posts = new HashMap<>();
		jdbc.sql("""
				SELECT p.id, p.season_id, p.author_id, p.content, p.important, p.pinned_at, p.edited_at, p.created_at,
				       u.first_name, u.last_name, u.status AS author_status, sm.title AS author_title, b.name AS author_branch
				FROM feed_post p JOIN app_user u ON u.id = p.author_id
				LEFT JOIN season_membership sm ON sm.season_id = p.season_id AND sm.user_id = p.author_id
				LEFT JOIN branch b ON b.id = sm.branch_id
				WHERE p.id IN (:ids)
				""").param("ids", ids).query(PostRow.class).list().forEach(r -> posts.put(r.id(), r));

		Map<Long, List<TargetRow>> targets = new HashMap<>();
		jdbc.sql("""
				SELECT t.post_id, t.target_type, t.target_id,
				       CASE t.target_type WHEN 'ALL' THEN 'Tüm Kurum' WHEN 'GROUP' THEN g.name WHEN 'BRANCH' THEN b.name
				            ELSE u.first_name || ' ' || u.last_name END AS name
				FROM feed_post_target t
				LEFT JOIN staff_group g ON t.target_type = 'GROUP' AND g.id = t.target_id
				LEFT JOIN branch b ON t.target_type = 'BRANCH' AND b.id = t.target_id
				LEFT JOIN app_user u ON t.target_type = 'USER' AND u.id = t.target_id
				WHERE t.post_id IN (:ids) ORDER BY t.id
				""").param("ids", ids).query(TargetRow.class).list()
				.forEach(r -> targets.computeIfAbsent(r.postId(), k -> new ArrayList<>()).add(r));

		Map<Long, List<FeedAttachment>> attachments = new HashMap<>();
		jdbc.sql("""
				SELECT a.post_id, f.id AS file_id, f.original_name, f.content_type, f.size_bytes
				FROM feed_post_attachment a JOIN stored_file f ON f.id = a.file_id
				WHERE a.post_id IN (:ids) ORDER BY a.post_id, a.sort_order
				""").param("ids", ids).query(AttachmentRow.class).list()
				.forEach(r -> attachments.computeIfAbsent(r.postId(), k -> new ArrayList<>())
						.add(new FeedAttachment(r.fileId().toString(), r.originalName(), r.contentType(),
								r.sizeBytes(), kind(r.contentType()))));

		Map<Long, LikeRow> likes = new HashMap<>();
		jdbc.sql("""
				SELECT post_id, count(*)::int AS cnt, bool_or(user_id = :uid) AS liked
				FROM feed_like WHERE post_id IN (:ids) GROUP BY post_id
				""").param("uid", uid).param("ids", ids).query(LikeRow.class).list()
				.forEach(r -> likes.put(r.postId(), r));

		Map<Long, Integer> commentCounts = new HashMap<>();
		jdbc.sql("""
				SELECT post_id, count(*)::int AS cnt FROM feed_comment
				WHERE post_id IN (:ids) AND deleted_at IS NULL GROUP BY post_id
				""").param("ids", ids).query(CountRow.class).list()
				.forEach(r -> commentCounts.put(r.postId(), r.cnt()));

		Map<Long, List<CommentDto>> latest = new HashMap<>();
		List<CommentRow> latestRows = jdbc.sql("""
				SELECT * FROM (
				  SELECT x.*, ROW_NUMBER() OVER (PARTITION BY x.post_id ORDER BY x.id DESC) AS rn FROM (
				""" + COMMENT_SELECT + """
				  WHERE c.post_id IN (:ids) AND c.deleted_at IS NULL) x
				) y WHERE y.rn <= 2 ORDER BY y.post_id, y.id ASC
				""").param("ids", ids).query(CommentRow.class).list();
		latestRows.forEach(r -> latest.computeIfAbsent(r.postId(), k -> new ArrayList<>()).add(toComment(r, me)));

		List<Long> importantIds = ids.stream().filter(id -> posts.get(id) != null && posts.get(id).important()).toList();
		Set<Long> readIds = new HashSet<>();
		if (!importantIds.isEmpty()) {
			readIds.addAll(jdbc.sql("SELECT post_id FROM feed_read WHERE user_id = :uid AND post_id IN (:ids)")
					.param("uid", uid).param("ids", importantIds).query(Long.class).list());
		}

		List<PostDto> result = new ArrayList<>();
		for (Long id : ids) {
			PostRow p = posts.get(id);
			if (p == null) {
				continue;
			}
			List<TargetRow> tr = targets.getOrDefault(id, List.of());
			List<TargetView> views = tr.stream().map(t -> new TargetView(TargetType.valueOf(t.targetType()),
					t.targetId(), t.name() == null ? "" : t.name())).toList();
			boolean mineAuthor = p.authorId() == uid;
			ReadStats stats = null;
			if (p.important() && mineAuthor) {
				stats = readStats(p, tr);
			}
			boolean archived = me.seasonId() == null || p.seasonId() != me.seasonId();
			LikeRow like = likes.get(id);
			PostDto dto = new PostDto(p.id(), p.seasonId(), archived,
					author(p.authorId(), p.firstName(), p.lastName(), p.authorStatus(), p.authorTitle(), p.authorBranch()),
					p.content(), p.important(), p.pinnedAt() != null, p.editedAt(), p.createdAt(), views,
					attachments.getOrDefault(id, List.of()), like == null ? 0 : like.cnt(),
					like != null && like.liked(), commentCounts.getOrDefault(id, 0),
					latest.getOrDefault(id, List.of()),
					p.important() && !mineAuthor ? readIds.contains(id) : null, stats,
					!archived && mineAuthor, !archived && (mineAuthor || me.isManager()),
					!archived && me.isManager());
			result.add(dto);
		}
		return result;
	}

	public Set<Long> recipients(PostRef post) {
		Set<Long> ids = new java.util.LinkedHashSet<>(audience.resolveUserIds(post.seasonId(), targetsOf(post.id())));
		ids.remove(post.authorId());
		return ids;
	}

	public Map<String, List<ReadUser>> readStatus(PostRef post) {
		Set<Long> recipients = recipients(post);
		Map<String, List<ReadUser>> result = new LinkedHashMap<>();
		result.put("read", new ArrayList<>());
		result.put("unread", new ArrayList<>());
		if (recipients.isEmpty()) {
			return result;
		}
		List<UserRow> rows = jdbc.sql("""
				SELECT u.id AS user_id, u.first_name || ' ' || u.last_name AS full_name, sm.title, b.name AS branch_name, r.read_at
				FROM app_user u
				LEFT JOIN season_membership sm ON sm.user_id = u.id AND sm.season_id = :sid
				LEFT JOIN branch b ON b.id = sm.branch_id
				LEFT JOIN feed_read r ON r.post_id = :pid AND r.user_id = u.id
				WHERE u.id IN (:ids)
				ORDER BY u.last_name COLLATE "tr-x-icu", u.first_name COLLATE "tr-x-icu", u.id
				""").param("sid", post.seasonId()).param("pid", post.id()).param("ids", recipients)
				.query(UserRow.class).list();
		for (UserRow r : rows) {
			ReadUser u = new ReadUser(r.userId(), r.fullName(), r.title(), r.branchName(), r.readAt());
			result.get(r.readAt() != null ? "read" : "unread").add(u);
		}
		return result;
	}

	public Collection<Long> unreadRecipients(PostRef post) {
		Set<Long> recipients = recipients(post);
		if (recipients.isEmpty()) {
			return List.of();
		}
		Set<Long> read = new HashSet<>(jdbc.sql("SELECT user_id FROM feed_read WHERE post_id = :pid AND user_id IN (:ids)")
				.param("pid", post.id()).param("ids", recipients).query(Long.class).list());
		return recipients.stream().filter(id -> !read.contains(id)).toList();
	}

	private ReadStats readStats(PostRow p, List<TargetRow> targets) {
		List<TargetRef> refs = targets.stream()
				.map(t -> new TargetRef(TargetType.valueOf(t.targetType()), t.targetId())).toList();
		Set<Long> recipients = new HashSet<>(audience.resolveUserIds(p.seasonId(), refs));
		recipients.remove(p.authorId());
		if (recipients.isEmpty()) {
			return new ReadStats(0, 0);
		}
		int read = jdbc.sql("SELECT count(*) FROM feed_read WHERE post_id = :pid AND user_id IN (:ids)")
				.param("pid", p.id()).param("ids", recipients).query(Integer.class).single();
		return new ReadStats(read, recipients.size());
	}

	private static CommentDto toComment(CommentRow r, UserContext me) {
		boolean archived = me.seasonId() == null || r.postSeasonId() != me.seasonId();
		boolean canDelete = !archived && (r.authorId() == me.userId() || r.postAuthorId() == me.userId() || me.isManager());
		return new CommentDto(r.id(), r.postId(),
				author(r.authorId(), r.firstName(), r.lastName(), r.authorStatus(), r.title(), r.branchName()),
				r.content(), r.createdAt(), canDelete);
	}

	private static FeedAuthor author(long id, String first, String last, String status, String title, String branch) {
		return new FeedAuthor(id, first + " " + last, title, branch, "PASSIVE".equals(status));
	}

	private static String kind(String contentType) {
		if (contentType.startsWith("image/")) {
			return "IMAGE";
		}
		return contentType.startsWith("video/") ? "VIDEO" : "FILE";
	}

}
