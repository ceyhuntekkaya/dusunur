package tr.okul.iletisim.dms;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.dms.DmsDtos.Crumb;
import tr.okul.iletisim.dms.DmsDtos.DmsPerson;
import tr.okul.iletisim.dms.DmsDtos.FileRow;
import tr.okul.iletisim.dms.DmsDtos.FolderContent;
import tr.okul.iletisim.dms.DmsDtos.FolderInfo;
import tr.okul.iletisim.dms.DmsDtos.FolderNode;
import tr.okul.iletisim.dms.DmsDtos.FolderRow;
import tr.okul.iletisim.dms.DmsDtos.GrantRow;
import tr.okul.iletisim.dms.DmsDtos.SearchResult;
import tr.okul.iletisim.dms.DmsDtos.SpaceRow;
import tr.okul.iletisim.dms.DmsDtos.TrashRow;

@Component
@Transactional(readOnly = true)
public class DmsQueries {

	public record TrashKey(String type, long id) {
	}

	private record SpaceDb(long id, String name, String description, long ownerId, String ownerName) {
	}

	private record CounterDb(long spaceId, long fileCount, long totalBytes, OffsetDateTime lastUpdate) {
	}

	private record GrantDb(long id, String subjectType, Long subjectId, int level, String subjectName) {
	}

	private record FolderDb(long id, String name, OffsetDateTime updatedAt, long personId, String personName,
			long itemCount) {
	}

	private record FileDb(long id, String name, long sizeBytes, String contentType, OffsetDateTime updatedAt,
			OffsetDateTime createdAt, long updatedBy, String updatedByName, long uploadedBy, String uploadedByName) {
	}

	private record PathNode(long id, Long parentId, String name) {
	}

	private record NodeDb(long id, Long parentId, String name, int depth) {
	}

	private record TrashDb(String type, long id, String name, Long containerId, OffsetDateTime deletedAt,
			long deletedBy, String deletedByName, Long sizeBytes) {
	}

	private record SearchDb(long id, String name, long spaceId, String spaceName, Long containerId, Long sizeBytes,
			String contentType) {
	}

	private static final String FILE_SELECT = """
			SELECT f.id, f.name, f.size_bytes, f.content_type, f.updated_at, f.created_at,
			       f.updated_by, uu.first_name || ' ' || uu.last_name AS updated_by_name,
			       f.uploaded_by, up.first_name || ' ' || up.last_name AS uploaded_by_name
			FROM dms_file f
			JOIN app_user uu ON uu.id = f.updated_by
			JOIN app_user up ON up.id = f.uploaded_by
			""";

	private static final String FOLDER_SELECT = """
			SELECT f.id, f.name, f.updated_at, f.created_by AS person_id,
			       u.first_name || ' ' || u.last_name AS person_name,
			       (SELECT count(*) FROM dms_folder c WHERE c.parent_id = f.id AND c.deleted_at IS NULL)
			     + (SELECT count(*) FROM dms_file x WHERE x.folder_id = f.id AND x.deleted_at IS NULL) AS item_count
			FROM dms_folder f
			JOIN app_user u ON u.id = f.created_by
			""";

	private final JdbcClient jdbc;
	private final DmsAccess access;
	private final AppProperties app;

	public DmsQueries(JdbcClient jdbc, DmsAccess access, AppProperties app) {
		this.jdbc = jdbc;
		this.access = access;
		this.app = app;
	}

	public List<SpaceRow> spaces(UserContext me) {
		Map<Long, DmsLevel> levels = access.accessibleSpaceLevels(me);
		return spaceRows(levels);
	}

	public SpaceRow space(long spaceId, DmsLevel level) {
		return spaceRows(Map.of(spaceId, level)).stream().findFirst()
				.orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_SPACE_NOT_FOUND));
	}

	private List<SpaceRow> spaceRows(Map<Long, DmsLevel> levels) {
		if (levels.isEmpty()) {
			return List.of();
		}
		List<SpaceDb> spaces = jdbc.sql("""
				SELECT s.id, s.name, s.description, s.owner_id, u.first_name || ' ' || u.last_name AS owner_name
				FROM dms_space s JOIN app_user u ON u.id = s.owner_id
				WHERE s.id IN (:ids)
				ORDER BY s.name COLLATE "tr-x-icu"
				""").param("ids", levels.keySet()).query(SpaceDb.class).list();
		Map<Long, CounterDb> counters = new HashMap<>();
		jdbc.sql("""
				SELECT space_id, count(*) AS file_count, COALESCE(sum(size_bytes), 0) AS total_bytes, max(updated_at) AS last_update
				FROM dms_file WHERE deleted_at IS NULL AND space_id IN (:ids) GROUP BY space_id
				""").param("ids", levels.keySet()).query(CounterDb.class).list()
				.forEach(c -> counters.put(c.spaceId(), c));
		List<SpaceRow> rows = new ArrayList<>();
		for (SpaceDb s : spaces) {
			CounterDb c = counters.get(s.id());
			rows.add(new SpaceRow(s.id(), s.name(), s.description(), new DmsPerson(s.ownerId(), s.ownerName()),
					levels.get(s.id()), c == null ? 0 : c.fileCount(), c == null ? 0 : c.totalBytes(),
					c == null ? null : c.lastUpdate()));
		}
		return rows;
	}

	private static final String GRANT_SELECT = """
			SELECT g.id, g.subject_type, g.subject_id, g.level,
			       CASE g.subject_type
			         WHEN 'USER' THEN (SELECT u.first_name || ' ' || u.last_name FROM app_user u WHERE u.id = g.subject_id)
			         WHEN 'GROUP' THEN (SELECT sg.name FROM staff_group sg WHERE sg.id = g.subject_id)
			         WHEN 'BRANCH' THEN (SELECT b.name FROM branch b WHERE b.id = g.subject_id)
			         ELSE 'Tüm Kurum' END AS subject_name
			FROM dms_space_grant g
			""";

	public List<GrantRow> grants(long spaceId) {
		return jdbc.sql("SELECT * FROM (" + GRANT_SELECT + " WHERE g.space_id = :sid) x "
				+ "ORDER BY CASE x.subject_type WHEN 'ALL' THEN 0 WHEN 'BRANCH' THEN 1 WHEN 'GROUP' THEN 2 ELSE 3 END, "
				+ "x.subject_name COLLATE \"tr-x-icu\"")
				.param("sid", spaceId).query(GrantDb.class).list().stream().map(DmsQueries::grantRow).toList();
	}

	public GrantRow grant(long grantId) {
		return grantRow(jdbc.sql(GRANT_SELECT + " WHERE g.id = :id").param("id", grantId).query(GrantDb.class).single());
	}

	private static GrantRow grantRow(GrantDb g) {
		return new GrantRow(g.id(), new TargetView(TargetType.valueOf(g.subjectType()), g.subjectId(), g.subjectName()),
				DmsLevel.ofRank(g.level()));
	}

	public FolderContent content(long spaceId, DmsLevel level, Long folderId) {
		SpaceRow space = space(spaceId, level);
		FolderInfo info = null;
		List<Crumb> crumbs = List.of();
		if (folderId != null) {
			PathNode f = jdbc.sql("SELECT id, parent_id, name FROM dms_folder WHERE id = :id AND space_id = :sid AND deleted_at IS NULL")
					.param("id", folderId).param("sid", spaceId).query(PathNode.class).optional()
					.orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
			info = new FolderInfo(f.id(), f.name(), f.parentId());
			crumbs = jdbc.sql("""
					WITH RECURSIVE up AS (
					  SELECT id, parent_id, name, 1 AS lvl FROM dms_folder WHERE id = :folderId
					  UNION ALL
					  SELECT f.id, f.parent_id, f.name, up.lvl + 1 FROM dms_folder f JOIN up ON f.id = up.parent_id)
					SELECT id, name FROM up ORDER BY lvl DESC
					""").param("folderId", folderId).query((rs, i) -> new Crumb(rs.getLong("id"), rs.getString("name"))).list();
		}
		String parentCond = folderId == null ? "f.parent_id IS NULL" : "f.parent_id = :pid";
		JdbcClient.StatementSpec folderSpec = jdbc.sql(FOLDER_SELECT + " WHERE f.space_id = :sid AND f.deleted_at IS NULL AND "
				+ parentCond + " ORDER BY f.name COLLATE \"tr-x-icu\"").param("sid", spaceId);
		String fileCond = folderId == null ? "f.folder_id IS NULL" : "f.folder_id = :pid";
		JdbcClient.StatementSpec fileSpec = jdbc.sql(FILE_SELECT + " WHERE f.space_id = :sid AND f.deleted_at IS NULL AND "
				+ fileCond + " ORDER BY f.name COLLATE \"tr-x-icu\"").param("sid", spaceId);
		if (folderId != null) {
			folderSpec = folderSpec.param("pid", folderId);
			fileSpec = fileSpec.param("pid", folderId);
		}
		List<FolderRow> folders = folderSpec.query(FolderDb.class).list().stream().map(DmsQueries::folderRow).toList();
		List<FileRow> files = fileSpec.query(FileDb.class).list().stream().map(DmsQueries::fileRow).toList();
		return new FolderContent(space, info, crumbs, folders, files);
	}

	public FolderRow folderRow(long id) {
		return folderRow(jdbc.sql(FOLDER_SELECT + " WHERE f.id = :id").param("id", id).query(FolderDb.class).single());
	}

	public FileRow fileRow(long id) {
		return fileRow(jdbc.sql(FILE_SELECT + " WHERE f.id = :id").param("id", id).query(FileDb.class).single());
	}

	private static FolderRow folderRow(FolderDb f) {
		return new FolderRow(f.id(), f.name(), f.updatedAt(), new DmsPerson(f.personId(), f.personName()),
				(int) f.itemCount());
	}

	private static FileRow fileRow(FileDb f) {
		return new FileRow(f.id(), f.name(), f.sizeBytes(), f.contentType(), isPreviewable(f.contentType()),
				f.updatedAt(), new DmsPerson(f.updatedBy(), f.updatedByName()),
				new DmsPerson(f.uploadedBy(), f.uploadedByName()), f.createdAt());
	}

	private static final java.util.Set<String> PREVIEWABLE = java.util.Set.of("image/jpeg", "image/png", "image/gif",
			"image/webp", "application/pdf");

	public static boolean isPreviewable(String contentType) {
		return contentType != null && PREVIEWABLE.contains(contentType);
	}

	public List<FolderNode> folderNodes(long spaceId) {
		return jdbc.sql("""
				SELECT id, parent_id, name, depth FROM dms_folder
				WHERE space_id = :sid AND deleted_at IS NULL
				ORDER BY name COLLATE "tr-x-icu"
				""").param("sid", spaceId).query(NodeDb.class).list().stream()
				.map(n -> new FolderNode(n.id(), n.parentId(), n.name(), n.depth())).toList();
	}

	public List<TrashRow> trash(long spaceId) {
		String spaceName = jdbc.sql("SELECT name FROM dms_space WHERE id = :id").param("id", spaceId)
				.query(String.class).single();
		Map<Long, PathNode> nodes = pathNodes(spaceId);
		int days = app.dms().trashRetentionDays();
		return jdbc.sql("""
				SELECT 'FOLDER' AS type, f.id, f.name, f.parent_id AS container_id, f.deleted_at, f.deleted_by,
				       u.first_name || ' ' || u.last_name AS deleted_by_name, NULL::bigint AS size_bytes
				FROM dms_folder f JOIN app_user u ON u.id = f.deleted_by
				WHERE f.space_id = :sid AND f.trash_root
				UNION ALL
				SELECT 'FILE' AS type, f.id, f.name, f.folder_id AS container_id, f.deleted_at, f.deleted_by,
				       u.first_name || ' ' || u.last_name AS deleted_by_name, f.size_bytes
				FROM dms_file f JOIN app_user u ON u.id = f.deleted_by
				WHERE f.space_id = :sid AND f.trash_root
				ORDER BY deleted_at DESC
				""").param("sid", spaceId).query(TrashDb.class).list().stream()
				.map(t -> new TrashRow(t.type(), t.id(), t.name(), path(spaceName, nodes, t.containerId()),
						new DmsPerson(t.deletedBy(), t.deletedByName()), t.deletedAt(),
						t.deletedAt().plusDays(days), t.sizeBytes()))
				.toList();
	}

	public List<SearchResult> search(UserContext me, String q) {
		String trimmed = TextNormalizer.truncateQuery(q);
		if (trimmed.length() < 2) {
			return List.of();
		}
		Collection<Long> ids = access.accessibleSpaceLevels(me).keySet();
		if (ids.isEmpty()) {
			return List.of();
		}
		String pattern = TextNormalizer.likePattern(trimmed);
		List<SearchDb> folders = jdbc.sql("""
				SELECT f.id, f.name, f.space_id, s.name AS space_name, f.parent_id AS container_id,
				       NULL::bigint AS size_bytes, NULL::varchar AS content_type
				FROM dms_folder f JOIN dms_space s ON s.id = f.space_id
				WHERE f.deleted_at IS NULL AND f.space_id IN (:ids) AND f.search_key LIKE :q
				ORDER BY f.name COLLATE "tr-x-icu" LIMIT 25
				""").param("ids", ids).param("q", pattern).query(SearchDb.class).list();
		List<SearchDb> files = jdbc.sql("""
				SELECT f.id, f.name, f.space_id, s.name AS space_name, f.folder_id AS container_id,
				       f.size_bytes, f.content_type
				FROM dms_file f JOIN dms_space s ON s.id = f.space_id
				WHERE f.deleted_at IS NULL AND f.space_id IN (:ids) AND f.search_key LIKE :q
				ORDER BY f.name COLLATE "tr-x-icu" LIMIT 25
				""").param("ids", ids).param("q", pattern).query(SearchDb.class).list();
		Map<Long, Map<Long, PathNode>> cache = new HashMap<>();
		List<SearchResult> result = new ArrayList<>();
		for (SearchDb f : folders) {
			Map<Long, PathNode> nodes = cache.computeIfAbsent(f.spaceId(), this::pathNodes);
			result.add(new SearchResult("FOLDER", f.id(), f.name(), f.spaceId(), f.spaceName(), f.containerId(),
					path(f.spaceName(), nodes, f.containerId()), null, null));
		}
		for (SearchDb f : files) {
			Map<Long, PathNode> nodes = cache.computeIfAbsent(f.spaceId(), this::pathNodes);
			result.add(new SearchResult("FILE", f.id(), f.name(), f.spaceId(), f.spaceName(), f.containerId(),
					path(f.spaceName(), nodes, f.containerId()), f.sizeBytes(), f.contentType()));
		}
		return result;
	}

	public List<SearchResult> recentFiles(UserContext me, int limit) {
		Collection<Long> ids = access.accessibleSpaceLevels(me).keySet();
		if (ids.isEmpty()) {
			return List.of();
		}
		List<SearchDb> files = jdbc.sql("""
				SELECT f.id, f.name, f.space_id, s.name AS space_name, f.folder_id AS container_id,
				       f.size_bytes, f.content_type
				FROM dms_file f JOIN dms_space s ON s.id = f.space_id
				WHERE f.deleted_at IS NULL AND f.space_id IN (:ids)
				ORDER BY f.created_at DESC, f.id DESC LIMIT :lim
				""").param("ids", ids).param("lim", limit).query(SearchDb.class).list();
		Map<Long, Map<Long, PathNode>> cache = new HashMap<>();
		List<SearchResult> result = new ArrayList<>();
		for (SearchDb f : files) {
			Map<Long, PathNode> nodes = cache.computeIfAbsent(f.spaceId(), this::pathNodes);
			result.add(new SearchResult("FILE", f.id(), f.name(), f.spaceId(), f.spaceName(), f.containerId(),
					path(f.spaceName(), nodes, f.containerId()), f.sizeBytes(), f.contentType()));
		}
		return result;
	}

	public List<TrashKey> expiredTrash(OffsetDateTime cutoff) {
		return jdbc.sql("""
				SELECT 'FOLDER' AS type, id FROM dms_folder WHERE trash_root AND deleted_at < :cutoff
				UNION ALL
				SELECT 'FILE' AS type, id FROM dms_file WHERE trash_root AND deleted_at < :cutoff
				""").param("cutoff", cutoff).query(TrashKey.class).list();
	}

	private Map<Long, PathNode> pathNodes(long spaceId) {
		Map<Long, PathNode> nodes = new HashMap<>();
		jdbc.sql("SELECT id, parent_id, name FROM dms_folder WHERE space_id = :sid").param("sid", spaceId)
				.query(PathNode.class).list().forEach(n -> nodes.put(n.id(), n));
		return nodes;
	}

	private static String path(String spaceName, Map<Long, PathNode> nodes, Long folderId) {
		List<String> parts = new ArrayList<>();
		Long cursor = folderId;
		while (cursor != null) {
			PathNode n = nodes.get(cursor);
			if (n == null) {
				break;
			}
			parts.add(0, n.name());
			cursor = n.parentId();
		}
		parts.add(0, spaceName);
		return String.join(" / ", parts);
	}

}
