package tr.okul.iletisim.dms;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.dms.DmsDtos.FileRow;
import tr.okul.iletisim.dms.DmsDtos.FileSaveResult;
import tr.okul.iletisim.dms.DmsDtos.FolderContent;
import tr.okul.iletisim.dms.DmsDtos.FolderCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.FolderNode;
import tr.okul.iletisim.dms.DmsDtos.FolderRow;
import tr.okul.iletisim.dms.DmsDtos.LocateResponse;
import tr.okul.iletisim.dms.DmsDtos.MoveRequest;
import tr.okul.iletisim.dms.DmsDtos.OnConflict;
import tr.okul.iletisim.dms.DmsDtos.PurgeResponse;
import tr.okul.iletisim.dms.DmsDtos.RenameRequest;
import tr.okul.iletisim.dms.DmsDtos.RestoreResult;
import tr.okul.iletisim.dms.DmsDtos.SaveFileRequest;
import tr.okul.iletisim.dms.DmsDtos.SearchResult;
import tr.okul.iletisim.dms.DmsDtos.TrashRow;

@Service
public class DmsItemService {

	public record FileContent(StoredFile file, boolean inline) {
	}

	private static final Logger log = LoggerFactory.getLogger(DmsItemService.class);
	private static final int MAX_DEPTH = 10;

	private static final String SUBTREE_ALL = """
			WITH RECURSIVE sub AS (
			  SELECT id FROM dms_folder WHERE id = :id
			  UNION ALL
			  SELECT c.id FROM dms_folder c JOIN sub ON c.parent_id = sub.id)
			""";
	private static final String SUBTREE_ACTIVE = """
			WITH RECURSIVE sub AS (
			  SELECT id FROM dms_folder WHERE id = :id
			  UNION ALL
			  SELECT c.id FROM dms_folder c JOIN sub ON c.parent_id = sub.id WHERE c.deleted_at IS NULL)
			""";

	private final DmsFolderRepository folders;
	private final DmsFileRepository files;
	private final DmsAccess access;
	private final DmsNameRules names;
	private final DmsQueries queries;
	private final FileStorageService storage;
	private final StoredFileRepository storedFiles;
	private final AuditService audit;
	private final JdbcClient jdbc;
	private final PlatformTransactionManager txManager;

	public DmsItemService(DmsFolderRepository folders, DmsFileRepository files, DmsAccess access, DmsNameRules names,
			DmsQueries queries, FileStorageService storage, StoredFileRepository storedFiles, AuditService audit,
			JdbcClient jdbc, PlatformTransactionManager txManager) {
		this.folders = folders;
		this.files = files;
		this.access = access;
		this.names = names;
		this.queries = queries;
		this.storage = storage;
		this.storedFiles = storedFiles;
		this.audit = audit;
		this.jdbc = jdbc;
		this.txManager = txManager;
	}

	@Transactional(readOnly = true)
	public FolderContent content(UserContext me, long spaceId, Long folderId) {
		me.requireMember();
		DmsLevel level = access.require(me, spaceId, DmsLevel.VIEW);
		return queries.content(spaceId, level, folderId);
	}

	@Transactional(readOnly = true)
	public List<FolderNode> folderNodes(UserContext me, long spaceId) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.VIEW);
		return queries.folderNodes(spaceId);
	}

	@Transactional(readOnly = true)
	public List<TrashRow> trash(UserContext me, long spaceId) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.EDIT);
		return queries.trash(spaceId);
	}

	@Transactional(readOnly = true)
	public List<SearchResult> search(UserContext me, String q) {
		me.requireMember();
		return queries.search(me, q);
	}

	@Transactional(readOnly = true)
	public List<SearchResult> recentFiles(UserContext me) {
		me.requireMember();
		return queries.recentFiles(me, 5);
	}

	@Transactional
	public FolderRow createFolder(UserContext me, long spaceId, FolderCreateRequest req) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.EDIT);
		String name = names.folderName(req.name());
		int depth = 1;
		if (req.parentId() != null) {
			DmsFolder parent = folders.findById(req.parentId())
					.filter(p -> p.getSpaceId().equals(spaceId) && p.getDeletedAt() == null)
					.orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
			depth = parent.getDepth() + 1;
		}
		if (depth > MAX_DEPTH) {
			throw new BusinessException(DmsErrorCode.DMS_DEPTH_LIMIT);
		}
		String key = names.key(name);
		if (nameTaken("dms_folder", "parent_id", spaceId, req.parentId(), key, null)) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_EXISTS, name);
		}
		DmsFolder f = new DmsFolder();
		f.setSpaceId(spaceId);
		f.setParentId(req.parentId());
		f.setName(name);
		f.setNameKey(key);
		f.setSearchKey(names.searchKey(name));
		f.setDepth((short) depth);
		f.setCreatedBy(me.userId());
		f = folders.saveAndFlush(f);
		return queries.folderRow(f.getId());
	}

	@Transactional
	public FolderRow renameFolder(UserContext me, long id, RenameRequest req) {
		DmsFolder f = folderFor(me, id, DmsLevel.EDIT);
		String name = names.folderName(req.name());
		String key = names.key(name);
		if (nameTaken("dms_folder", "parent_id", f.getSpaceId(), f.getParentId(), key, f.getId())) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_EXISTS, name);
		}
		f.setName(name);
		f.setNameKey(key);
		f.setSearchKey(names.searchKey(name));
		folders.saveAndFlush(f);
		return queries.folderRow(id);
	}

	@Transactional
	public void moveFolder(UserContext me, long id, MoveRequest req) {
		DmsFolder f = folderFor(me, id, DmsLevel.EDIT);
		Long targetId = req.targetFolderId();
		int baseDepth = 0;
		if (targetId != null) {
			DmsFolder t = folders.findById(targetId).orElse(null);
			if (t == null || !t.getSpaceId().equals(f.getSpaceId()) || t.getDeletedAt() != null
					|| isSameOrDescendant(t, f.getId())) {
				throw new BusinessException(DmsErrorCode.DMS_MOVE_INVALID);
			}
			baseDepth = t.getDepth();
		}
		Integer maxDepth = jdbc.sql(SUBTREE_ACTIVE + "SELECT max(f.depth) FROM dms_folder f WHERE f.id IN (SELECT id FROM sub)")
				.param("id", id).query(Integer.class).single();
		int newDepth = baseDepth + 1;
		int delta = newDepth - f.getDepth();
		if ((maxDepth == null ? f.getDepth() : maxDepth) + delta > MAX_DEPTH) {
			throw new BusinessException(DmsErrorCode.DMS_DEPTH_LIMIT);
		}
		if (nameTaken("dms_folder", "parent_id", f.getSpaceId(), targetId, f.getNameKey(), f.getId())) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_EXISTS, f.getName());
		}
		jdbc.sql("UPDATE dms_folder SET parent_id = :p, updated_at = now() WHERE id = :id")
				.param("p", targetId).param("id", id).update();
		shiftDepth(id, delta);
	}

	@Transactional
	public void trashFolder(UserContext me, long id) {
		DmsFolder f = folderFor(me, id, DmsLevel.EDIT);
		jdbc.sql(SUBTREE_ACTIVE + """
				UPDATE dms_file SET deleted_at = now(), deleted_by = :me, trash_root = false, trashed_with = :id
				WHERE deleted_at IS NULL AND folder_id IN (SELECT id FROM sub)
				""").param("id", id).param("me", me.userId()).update();
		jdbc.sql(SUBTREE_ACTIVE + """
				UPDATE dms_folder SET deleted_at = now(), deleted_by = :me, trash_root = false, trashed_with = :id
				WHERE id IN (SELECT id FROM sub) AND id <> :id
				""").param("id", id).param("me", me.userId()).update();
		jdbc.sql("UPDATE dms_folder SET deleted_at = now(), deleted_by = :me, trash_root = true, trashed_with = NULL WHERE id = :id")
				.param("id", id).param("me", me.userId()).update();
		audit.log(me.userId(), DmsAuditAction.DMS_ITEM_TRASHED, "DMS_FOLDER", id,
				detail(f.getSpaceId(), f.getName(), "FOLDER"));
	}

	@Transactional
	public FileSaveResult saveFile(UserContext me, long spaceId, SaveFileRequest req) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.EDIT);
		Long folderId = req.folderId();
		if (folderId != null) {
			folders.findById(folderId).filter(p -> p.getSpaceId().equals(spaceId) && p.getDeletedAt() == null)
					.orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
		}
		StoredFile sf = storedFiles.findById(req.fileId()).filter(f -> f.getDeletedAt() == null)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.FILE_NOT_USABLE));
		OnConflict mode = req.onConflict() == null ? OnConflict.ASK : req.onConflict();
		String name = names.sanitize(sf.getOriginalName());
		DmsFile existing = findFile(spaceId, folderId, names.key(name));

		if (existing != null && mode == OnConflict.ASK) {
			throw BusinessException.details(DmsErrorCode.DMS_NAME_CONFLICT, List.of(existing.getName()),
					new Object[] { existing.getName() });
		}
		if (existing != null && mode == OnConflict.OVERWRITE) {
			StoredFile attached = storage.attach(req.fileId(), me, FilePurpose.DMS);
			UUID old = existing.getFileId();
			existing.setFileId(attached.getId());
			existing.setSizeBytes(attached.getSizeBytes());
			existing.setContentType(attached.getContentType());
			existing.setUpdatedBy(me.userId());
			files.saveAndFlush(existing);
			deleteStoredAfterCommit(List.of(old));
			return new FileSaveResult(queries.fileRow(existing.getId()), false);
		}
		if (existing != null) {
			final Long fid = folderId;
			name = names.uniqueName(name, DmsNameRules.FILE_MAX,
					candidate -> nameTaken("dms_file", "folder_id", spaceId, fid, names.key(candidate), null));
		}
		StoredFile attached = storage.attach(req.fileId(), me, FilePurpose.DMS);
		DmsFile f = new DmsFile();
		f.setSpaceId(spaceId);
		f.setFolderId(folderId);
		f.setName(name);
		f.setNameKey(names.key(name));
		f.setSearchKey(names.searchKey(name));
		f.setFileId(attached.getId());
		f.setSizeBytes(attached.getSizeBytes());
		f.setContentType(attached.getContentType());
		f.setUploadedBy(me.userId());
		f.setUpdatedBy(me.userId());
		f = files.saveAndFlush(f);
		return new FileSaveResult(queries.fileRow(f.getId()), true);
	}

	@Transactional
	public FileRow renameFile(UserContext me, long id, RenameRequest req) {
		DmsFile f = fileFor(me, id, DmsLevel.EDIT);
		String name = names.fileName(req.name());
		if (!names.extension(name).equalsIgnoreCase(names.extension(f.getName()))) {
			throw new BusinessException(DmsErrorCode.DMS_EXTENSION_CHANGE);
		}
		String key = names.key(name);
		if (nameTaken("dms_file", "folder_id", f.getSpaceId(), f.getFolderId(), key, f.getId())) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_EXISTS, name);
		}
		f.setName(name);
		f.setNameKey(key);
		f.setSearchKey(names.searchKey(name));
		f.setUpdatedBy(me.userId());
		files.saveAndFlush(f);
		return queries.fileRow(id);
	}

	@Transactional
	public void moveFile(UserContext me, long id, MoveRequest req) {
		DmsFile f = fileFor(me, id, DmsLevel.EDIT);
		Long targetId = req.targetFolderId();
		if (targetId != null) {
			DmsFolder t = folders.findById(targetId).orElse(null);
			if (t == null || !t.getSpaceId().equals(f.getSpaceId()) || t.getDeletedAt() != null) {
				throw new BusinessException(DmsErrorCode.DMS_MOVE_INVALID);
			}
		}
		if (nameTaken("dms_file", "folder_id", f.getSpaceId(), targetId, f.getNameKey(), f.getId())) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_EXISTS, f.getName());
		}
		f.setFolderId(targetId);
		f.setUpdatedBy(me.userId());
		files.saveAndFlush(f);
	}

	@Transactional
	public void trashFile(UserContext me, long id) {
		DmsFile f = fileFor(me, id, DmsLevel.EDIT);
		f.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
		f.setDeletedBy(me.userId());
		f.setTrashRoot(true);
		f.setTrashedWith(null);
		files.saveAndFlush(f);
		audit.log(me.userId(), DmsAuditAction.DMS_ITEM_TRASHED, "DMS_FILE", id,
				detail(f.getSpaceId(), f.getName(), "FILE"));
	}

	@Transactional(readOnly = true)
	public FileContent content(UserContext me, long id, boolean download) {
		DmsFile f = fileFor(me, id, DmsLevel.VIEW);
		StoredFile stored = storage.get(f.getFileId());
		StoredFile copy = new StoredFile();
		copy.setId(stored.getId());
		copy.setOriginalName(f.getName());
		copy.setContentType(stored.getContentType());
		copy.setSizeBytes(stored.getSizeBytes());
		copy.setStoragePath(stored.getStoragePath());
		copy.setPurpose(stored.getPurpose());
		return new FileContent(copy, !download && DmsQueries.isPreviewable(stored.getContentType()));
	}

	@Transactional(readOnly = true)
	public LocateResponse locate(UserContext me, long id) {
		DmsFile f = fileFor(me, id, DmsLevel.VIEW);
		return new LocateResponse(f.getSpaceId(), f.getFolderId());
	}

	@Transactional
	public RestoreResult restore(UserContext me, String type, long id) {
		me.requireMember();
		boolean folder = isFolder(type);
		if (folder) {
			DmsFolder f = folders.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
			access.require(me, f.getSpaceId(), DmsLevel.EDIT);
			requireTrashRoot(f.isTrashRoot(), f.getDeletedAt());
			RestoreResult result = restoreFolder(f);
			audit.log(me.userId(), DmsAuditAction.DMS_ITEM_RESTORED, "DMS_FOLDER", id,
					detail(f.getSpaceId(), f.getName(), "FOLDER"));
			return result;
		}
		DmsFile f = files.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
		access.require(me, f.getSpaceId(), DmsLevel.EDIT);
		requireTrashRoot(f.isTrashRoot(), f.getDeletedAt());
		RestoreResult result = restoreFile(f);
		audit.log(me.userId(), DmsAuditAction.DMS_ITEM_RESTORED, "DMS_FILE", id,
				detail(f.getSpaceId(), f.getName(), "FILE"));
		return result;
	}

	@Transactional
	public void purge(UserContext me, String type, long id) {
		me.requireMember();
		boolean folder = isFolder(type);
		long spaceId;
		String name;
		if (folder) {
			DmsFolder f = folders.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
			access.require(me, f.getSpaceId(), DmsLevel.MANAGE);
			requireTrashRoot(f.isTrashRoot(), f.getDeletedAt());
			spaceId = f.getSpaceId();
			name = f.getName();
		}
		else {
			DmsFile f = files.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
			access.require(me, f.getSpaceId(), DmsLevel.MANAGE);
			requireTrashRoot(f.isTrashRoot(), f.getDeletedAt());
			spaceId = f.getSpaceId();
			name = f.getName();
		}
		purgeItem(folder, id);
		audit.log(me.userId(), DmsAuditAction.DMS_ITEM_PURGED, folder ? "DMS_FOLDER" : "DMS_FILE", id,
				detail(spaceId, name, folder ? "FOLDER" : "FILE"));
	}

	@Transactional
	public PurgeResponse emptyTrash(UserContext me, long spaceId) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.MANAGE);
		List<Long> folderIds = jdbc.sql("SELECT id FROM dms_folder WHERE space_id = :sid AND trash_root")
				.param("sid", spaceId).query(Long.class).list();
		List<Long> fileIds = jdbc.sql("SELECT id FROM dms_file WHERE space_id = :sid AND trash_root")
				.param("sid", spaceId).query(Long.class).list();
		fileIds.forEach(fid -> purgeItem(false, fid));
		folderIds.forEach(fid -> purgeItem(true, fid));
		int purged = folderIds.size() + fileIds.size();
		String spaceName = jdbc.sql("SELECT name FROM dms_space WHERE id = :id").param("id", spaceId)
				.query(String.class).single();
		Map<String, Object> d = detail(spaceId, spaceName, "SPACE");
		d.put("purged", purged);
		audit.log(me.userId(), DmsAuditAction.DMS_TRASH_EMPTIED, "DMS_SPACE", spaceId, d);
		return new PurgeResponse(purged);
	}

	@Transactional
	public void purgeExpired(DmsQueries.TrashKey key) {
		purgeItem(key.type().equals("FOLDER"), key.id());
	}

	private RestoreResult restoreFolder(DmsFolder f) {
		Long parent = f.getParentId();
		int baseDepth = 0;
		boolean toRoot = false;
		if (parent != null) {
			DmsFolder p = folders.findById(parent).orElse(null);
			if (p == null || p.getDeletedAt() != null) {
				parent = null;
				toRoot = true;
			}
			else {
				baseDepth = p.getDepth();
			}
		}
		if (parent != null) {
			Integer maxDepth = jdbc.sql("SELECT max(depth) FROM dms_folder WHERE trashed_with = :id")
					.param("id", f.getId()).query(Integer.class).single();
			int deepest = Math.max(f.getDepth(), maxDepth == null ? 0 : maxDepth);
			if (deepest + baseDepth + 1 - f.getDepth() > MAX_DEPTH) {
				parent = null;
				toRoot = true;
				baseDepth = 0;
			}
		}
		final Long target = parent;
		String name = names.uniqueName(f.getName(), DmsNameRules.FOLDER_MAX,
				c -> nameTaken("dms_folder", "parent_id", f.getSpaceId(), target, names.key(c), null));
		int delta = baseDepth + 1 - f.getDepth();
		jdbc.sql("UPDATE dms_file SET deleted_at = NULL, deleted_by = NULL, trash_root = false, trashed_with = NULL WHERE trashed_with = :id")
				.param("id", f.getId()).update();
		jdbc.sql("UPDATE dms_folder SET deleted_at = NULL, deleted_by = NULL, trash_root = false, trashed_with = NULL WHERE trashed_with = :id")
				.param("id", f.getId()).update();
		jdbc.sql("""
				UPDATE dms_folder SET deleted_at = NULL, deleted_by = NULL, trash_root = false, trashed_with = NULL,
				       parent_id = :p, name = :name, name_key = :key, search_key = :sk, updated_at = now()
				WHERE id = :id
				""").param("p", target).param("name", name).param("key", names.key(name))
				.param("sk", names.searchKey(name)).param("id", f.getId()).update();
		shiftDepth(f.getId(), delta);
		return new RestoreResult(toRoot, name.equals(f.getName()) ? null : name);
	}

	private RestoreResult restoreFile(DmsFile f) {
		Long folder = f.getFolderId();
		boolean toRoot = false;
		if (folder != null) {
			DmsFolder p = folders.findById(folder).orElse(null);
			if (p == null || p.getDeletedAt() != null) {
				folder = null;
				toRoot = true;
			}
		}
		final Long target = folder;
		String name = names.uniqueName(f.getName(), DmsNameRules.FILE_MAX,
				c -> nameTaken("dms_file", "folder_id", f.getSpaceId(), target, names.key(c), null));
		jdbc.sql("""
				UPDATE dms_file SET deleted_at = NULL, deleted_by = NULL, trash_root = false, trashed_with = NULL,
				       folder_id = :p, name = :name, name_key = :key, search_key = :sk, updated_at = now()
				WHERE id = :id
				""").param("p", target).param("name", name).param("key", names.key(name))
				.param("sk", names.searchKey(name)).param("id", f.getId()).update();
		return new RestoreResult(toRoot, name.equals(f.getName()) ? null : name);
	}

	private void purgeItem(boolean folder, long id) {
		if (!folder) {
			List<UUID> stored = jdbc.sql("SELECT file_id FROM dms_file WHERE id = :id").param("id", id)
					.query(UUID.class).list();
			jdbc.sql("DELETE FROM dms_file WHERE id = :id").param("id", id).update();
			deleteStoredAfterCommit(stored);
			return;
		}
		List<Long> ids = jdbc.sql(SUBTREE_ALL + "SELECT id FROM sub").param("id", id).query(Long.class).list();
		if (ids.isEmpty()) {
			return;
		}
		List<UUID> stored = jdbc.sql("SELECT file_id FROM dms_file WHERE folder_id IN (:ids)").param("ids", ids)
				.query(UUID.class).list();
		jdbc.sql("DELETE FROM dms_file WHERE folder_id IN (:ids)").param("ids", ids).update();
		jdbc.sql("DELETE FROM dms_folder WHERE id IN (:ids)").param("ids", ids).update();
		deleteStoredAfterCommit(stored);
	}

	private void deleteStoredAfterCommit(Collection<UUID> ids) {
		if (ids.isEmpty()) {
			return;
		}
		List<UUID> copy = List.copyOf(ids);
		RealtimePublisher.afterCommit(() -> {
			TransactionTemplate tx = new TransactionTemplate(txManager);
			tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			for (UUID fileId : copy) {
				try {
					tx.executeWithoutResult(s -> storedFiles.findById(fileId).filter(f -> f.getDeletedAt() == null)
							.ifPresent(storage::delete));
				}
				catch (RuntimeException e) {
					log.warn("Doküman dosyası silinemedi: {}", fileId, e);
				}
			}
		});
	}

	private void shiftDepth(long folderId, int delta) {
		if (delta == 0) {
			return;
		}
		jdbc.sql(SUBTREE_ALL + "UPDATE dms_folder SET depth = depth + :delta WHERE id IN (SELECT id FROM sub)")
				.param("id", folderId).param("delta", delta).update();
	}

	private boolean isSameOrDescendant(DmsFolder candidate, long ancestorId) {
		DmsFolder cursor = candidate;
		while (cursor != null) {
			if (cursor.getId() == ancestorId) {
				return true;
			}
			cursor = cursor.getParentId() == null ? null : folders.findById(cursor.getParentId()).orElse(null);
		}
		return false;
	}

	private boolean nameTaken(String table, String parentColumn, long spaceId, Long parentId, String key, Long excludeId) {
		String sql = "SELECT count(*) FROM " + table + " WHERE space_id = :sid AND deleted_at IS NULL AND name_key = :key AND "
				+ (parentId == null ? parentColumn + " IS NULL" : parentColumn + " = :pid")
				+ (excludeId == null ? "" : " AND id <> :ex");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("sid", spaceId).param("key", key);
		if (parentId != null) {
			spec = spec.param("pid", parentId);
		}
		if (excludeId != null) {
			spec = spec.param("ex", excludeId);
		}
		return spec.query(Long.class).single() > 0;
	}

	private DmsFile findFile(long spaceId, Long folderId, String key) {
		String sql = "SELECT id FROM dms_file WHERE space_id = :sid AND deleted_at IS NULL AND name_key = :key AND "
				+ (folderId == null ? "folder_id IS NULL" : "folder_id = :pid");
		JdbcClient.StatementSpec spec = jdbc.sql(sql).param("sid", spaceId).param("key", key);
		if (folderId != null) {
			spec = spec.param("pid", folderId);
		}
		return spec.query(Long.class).optional().flatMap(files::findById).orElse(null);
	}

	private DmsFolder folderFor(UserContext me, long id, DmsLevel min) {
		me.requireMember();
		DmsFolder f = folders.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
		access.require(me, f.getSpaceId(), min);
		if (f.getDeletedAt() != null) {
			throw new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND);
		}
		return f;
	}

	private DmsFile fileFor(UserContext me, long id, DmsLevel min) {
		me.requireMember();
		DmsFile f = files.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND));
		access.require(me, f.getSpaceId(), min);
		if (f.getDeletedAt() != null) {
			throw new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND);
		}
		return f;
	}

	private static void requireTrashRoot(boolean trashRoot, OffsetDateTime deletedAt) {
		if (!trashRoot || deletedAt == null) {
			throw new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND);
		}
	}

	private static boolean isFolder(String type) {
		String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
		if (t.equals("folder")) {
			return true;
		}
		if (t.equals("file")) {
			return false;
		}
		throw new BusinessException(DmsErrorCode.DMS_ITEM_NOT_FOUND);
	}

	private static Map<String, Object> detail(long spaceId, String name, String type) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("spaceId", spaceId);
		d.put("name", name);
		d.put("type", type);
		return d;
	}

}
