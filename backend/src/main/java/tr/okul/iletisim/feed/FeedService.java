package tr.okul.iletisim.feed;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;
import tr.okul.iletisim.core.directory.AudienceResolver;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.feed.FeedDtos.CommentDto;
import tr.okul.iletisim.feed.FeedDtos.CreatePostRequest;
import tr.okul.iletisim.feed.FeedDtos.CreatePostResponse;
import tr.okul.iletisim.feed.FeedDtos.FeedSeason;
import tr.okul.iletisim.feed.FeedDtos.PostDto;
import tr.okul.iletisim.feed.FeedDtos.UpdatePostRequest;
import tr.okul.iletisim.feed.FeedQueries.PostRef;

@Service
public class FeedService {

	private static final Set<TargetType> MANAGER_TYPES = Set.of(TargetType.ALL, TargetType.BRANCH, TargetType.GROUP,
			TargetType.USER);
	private static final Set<TargetType> STAFF_TYPES = Set.of(TargetType.GROUP, TargetType.USER);

	private final FeedPostRepository posts;
	private final FeedPostTargetRepository targetRepository;
	private final FeedPostAttachmentRepository attachmentRepository;
	private final FeedQueries queries;
	private final AudienceResolver audience;
	private final FileStorageService files;
	private final StoredFileRepository fileRepository;
	private final NotificationService notifications;
	private final RealtimePublisher realtime;
	private final AuditService audit;

	public FeedService(FeedPostRepository posts, FeedPostTargetRepository targetRepository,
			FeedPostAttachmentRepository attachmentRepository, FeedQueries queries, AudienceResolver audience,
			FileStorageService files, StoredFileRepository fileRepository, NotificationService notifications,
			RealtimePublisher realtime, AuditService audit) {
		this.posts = posts;
		this.targetRepository = targetRepository;
		this.attachmentRepository = attachmentRepository;
		this.queries = queries;
		this.audience = audience;
		this.files = files;
		this.fileRepository = fileRepository;
		this.notifications = notifications;
		this.realtime = realtime;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public List<FeedSeason> seasons(UserContext me) {
		me.requireMember();
		return queries.seasons(me.userId());
	}

	@Transactional(readOnly = true)
	public CursorResponse<PostDto> list(UserContext me, Long seasonId, String filter, Long groupId, Long before,
			int size) {
		me.requireMember();
		int limit = Math.max(1, Math.min(size, 30));
		return queries.page(me, seasonId, filter, groupId, before, limit);
	}

	@Transactional(readOnly = true)
	public List<PostDto> pinned(UserContext me) {
		return queries.pinned(me);
	}

	@Transactional(readOnly = true)
	public PostDto detail(UserContext me, long id) {
		me.requireMember();
		queries.assertVisible(id, me.userId());
		return queries.loadCards(List.of(id), me).get(0);
	}

	@Transactional(readOnly = true)
	public CursorResponse<CommentDto> comments(UserContext me, long id, Long after, int size) {
		me.requireMember();
		queries.assertVisible(id, me.userId());
		return queries.comments(me, id, after, Math.max(1, Math.min(size, 50)));
	}

	@Transactional(readOnly = true)
	public StoredFile file(UserContext me, long id, UUID fileId) {
		me.requireMember();
		queries.assertVisible(id, me.userId());
		if (!queries.hasAttachment(id, fileId)) {
			throw new BusinessException(CommonErrorCode.FILE_NOT_FOUND);
		}
		return files.get(fileId);
	}

	@Transactional
	public CreatePostResponse create(UserContext me, CreatePostRequest request) {
		long seasonId = me.requireMember();
		List<TargetRef> requested = request.targets() == null ? List.of() : request.targets();
		if (requested.size() > 50) {
			throw new BusinessException(FeedErrorCode.FEED_TOO_MANY_TARGETS);
		}
		String content = FeedText.trimToNull(request.content());
		Set<UUID> fileIds = new LinkedHashSet<>(request.fileIds() == null ? List.of() : request.fileIds());
		if (fileIds.size() > 10) {
			throw new BusinessException(FeedErrorCode.FEED_TOO_MANY_FILES);
		}
		if (content == null && fileIds.isEmpty()) {
			throw new BusinessException(FeedErrorCode.FEED_EMPTY_POST);
		}
		if (request.important() && !me.isManager()) {
			throw new BusinessException(FeedErrorCode.FEED_IMPORTANT_FORBIDDEN);
		}

		Set<TargetRef> targets = new LinkedHashSet<>();
		for (TargetRef t : requested) {
			if (t != null && t.type() == TargetType.USER && t.id() != null && t.id() == me.userId()) {
				continue;
			}
			targets.add(t);
		}
		audience.validate(me, targets, me.isManager() ? MANAGER_TYPES : STAFF_TYPES);
		if (targets.stream().anyMatch(t -> t.type() == TargetType.ALL)) {
			targets = Set.of(new TargetRef(TargetType.ALL, null));
		}
		if (!me.isManager()) {
			for (TargetRef t : targets) {
				if (t.type() == TargetType.GROUP && !me.groupIds().contains(t.id())) {
					throw new BusinessException(FeedErrorCode.FEED_GROUP_NOT_MEMBER);
				}
			}
		}

		FeedPost post = new FeedPost();
		post.setSeasonId(seasonId);
		post.setAuthorId(me.userId());
		post.setContent(content);
		post.setImportant(request.important());
		post = posts.saveAndFlush(post);
		long postId = post.getId();

		List<FeedPostTarget> targetRows = new ArrayList<>();
		for (TargetRef t : targets) {
			FeedPostTarget row = new FeedPostTarget();
			row.setPostId(postId);
			row.setTargetType(t.type());
			row.setTargetId(t.id());
			targetRows.add(row);
		}
		targetRepository.saveAllAndFlush(targetRows);

		List<FeedPostAttachment> attachmentRows = new ArrayList<>();
		short order = 0;
		for (UUID fileId : fileIds) {
			files.attach(fileId, me, FilePurpose.FEED);
			attachmentRows.add(new FeedPostAttachment(postId, fileId, order++));
		}
		attachmentRepository.saveAllAndFlush(attachmentRows);

		Set<Long> recipients = new LinkedHashSet<>(audience.resolveUserIds(seasonId, targets));
		recipients.remove(me.userId());
		String body = content != null ? FeedText.snippet(content, 120) : fileIds.size() + " ek dosya";
		String link = "/akis/" + postId;
		if (request.important()) {
			notifications.notify(recipients, "FEED_IMPORTANT", "Önemli duyuru: " + me.fullName(), body, link);
		}
		else {
			List<TargetRef> userTargets = targets.stream().filter(t -> t.type() == TargetType.USER).toList();
			if (!userTargets.isEmpty()) {
				Set<Long> direct = new LinkedHashSet<>(audience.resolveUserIds(seasonId, userTargets));
				direct.remove(me.userId());
				notifications.notify(direct, "FEED_DIRECT", me.fullName() + " sizinle bir paylaşım yaptı", body, link);
			}
		}
		realtime.toUsers(recipients, "FEED_NEW_POST", Map.of("postId", postId, "seasonId", seasonId));
		if (request.important()) {
			realtime.toUsers(recipients, "BADGES", Map.of());
		}
		return new CreatePostResponse(queries.loadCards(List.of(postId), me).get(0), recipients.size());
	}

	@Transactional
	public PostDto update(UserContext me, long id, UpdatePostRequest request) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		if (ref.authorId() != me.userId()) {
			throw new BusinessException(FeedErrorCode.FEED_EDIT_FORBIDDEN);
		}
		String content = FeedText.trimToNull(request.content());
		if (content == null && queries.attachmentFileIds(id).isEmpty()) {
			throw new BusinessException(FeedErrorCode.FEED_EMPTY_POST);
		}
		FeedPost post = posts.findById(id).orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_POST_NOT_FOUND));
		post.setContent(content);
		post.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC));
		posts.saveAndFlush(post);
		return queries.loadCards(List.of(id), me).get(0);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		me.requireWritable(ref.seasonId());
		boolean author = ref.authorId() == me.userId();
		if (!author && !me.isManager()) {
			throw new BusinessException(FeedErrorCode.FEED_DELETE_FORBIDDEN);
		}
		FeedPost post = posts.findById(id).orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_POST_NOT_FOUND));
		post.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
		post.setDeletedBy(me.userId());
		posts.saveAndFlush(post);
		for (UUID fileId : queries.attachmentFileIds(id)) {
			fileRepository.findById(fileId).filter(f -> f.getDeletedAt() == null).ifPresent(files::delete);
		}
		if (ref.important()) {
			realtime.toUsers(queries.recipients(ref), "BADGES", Map.of());
		}
		if (!author) {
			audit.log(me.userId(), FeedAuditAction.FEED_POST_DELETED, "FEED_POST", id,
					Map.of("authorId", ref.authorId(), "snippet", FeedText.snippet(ref.content(), 120)));
		}
	}

	@Transactional
	public void pin(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		if (!me.isManager()) {
			throw new BusinessException(FeedErrorCode.FEED_PIN_FORBIDDEN);
		}
		me.requireWritable(ref.seasonId());
		if (ref.pinnedAt() != null) {
			return;
		}
		int pinned = posts.countPinned(ref.seasonId());
		if (pinned >= 3) {
			throw new BusinessException(FeedErrorCode.FEED_PIN_LIMIT);
		}
		FeedPost post = posts.findById(id).orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_POST_NOT_FOUND));
		post.setPinnedAt(OffsetDateTime.now(ZoneOffset.UTC));
		post.setPinnedBy(me.userId());
		posts.save(post);
		audit.log(me.userId(), FeedAuditAction.FEED_POST_PINNED, "FEED_POST", id, Map.of());
	}

	@Transactional
	public void unpin(UserContext me, long id) {
		me.requireMember();
		PostRef ref = queries.assertVisible(id, me.userId());
		if (!me.isManager()) {
			throw new BusinessException(FeedErrorCode.FEED_PIN_FORBIDDEN);
		}
		me.requireWritable(ref.seasonId());
		if (ref.pinnedAt() == null) {
			return;
		}
		FeedPost post = posts.findById(id).orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_POST_NOT_FOUND));
		post.setPinnedAt(null);
		post.setPinnedBy(null);
		posts.save(post);
		audit.log(me.userId(), FeedAuditAction.FEED_POST_UNPINNED, "FEED_POST", id, Map.of());
	}

}
