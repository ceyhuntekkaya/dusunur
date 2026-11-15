package tr.okul.iletisim.dms;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.dms.DmsDtos.FileRow;
import tr.okul.iletisim.dms.DmsDtos.FileSaveResult;
import tr.okul.iletisim.dms.DmsDtos.FolderContent;
import tr.okul.iletisim.dms.DmsDtos.FolderCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.FolderNode;
import tr.okul.iletisim.dms.DmsDtos.FolderRow;
import tr.okul.iletisim.dms.DmsDtos.GrantCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.GrantResult;
import tr.okul.iletisim.dms.DmsDtos.GrantRow;
import tr.okul.iletisim.dms.DmsDtos.GrantUpdateRequest;
import tr.okul.iletisim.dms.DmsDtos.LocateResponse;
import tr.okul.iletisim.dms.DmsDtos.MoveRequest;
import tr.okul.iletisim.dms.DmsDtos.PurgeResponse;
import tr.okul.iletisim.dms.DmsDtos.RenameRequest;
import tr.okul.iletisim.dms.DmsDtos.RestoreResult;
import tr.okul.iletisim.dms.DmsDtos.SaveFileRequest;
import tr.okul.iletisim.dms.DmsDtos.SearchResult;
import tr.okul.iletisim.dms.DmsDtos.SpaceRequest;
import tr.okul.iletisim.dms.DmsDtos.SpaceRow;
import tr.okul.iletisim.dms.DmsDtos.TrashRow;

@RestController
public class DmsController {

	private final DmsSpaceService spaces;
	private final DmsItemService items;
	private final FileStorageService storage;

	public DmsController(DmsSpaceService spaces, DmsItemService items, FileStorageService storage) {
		this.spaces = spaces;
		this.items = items;
		this.storage = storage;
	}

	@GetMapping("/api/dms/spaces")
	public List<SpaceRow> spaces(UserContext me) {
		return spaces.list(me);
	}

	@PostMapping("/api/dms/spaces")
	@ResponseStatus(HttpStatus.CREATED)
	public SpaceRow createSpace(UserContext me, @Valid @RequestBody SpaceRequest request) {
		return spaces.create(me, request);
	}

	@GetMapping("/api/dms/spaces/{id}")
	public SpaceRow space(UserContext me, @PathVariable long id) {
		return spaces.get(me, id);
	}

	@PutMapping("/api/dms/spaces/{id}")
	public SpaceRow updateSpace(UserContext me, @PathVariable long id, @Valid @RequestBody SpaceRequest request) {
		return spaces.update(me, id, request);
	}

	@DeleteMapping("/api/dms/spaces/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSpace(UserContext me, @PathVariable long id) {
		spaces.delete(me, id);
	}

	@GetMapping("/api/dms/spaces/{id}/grants")
	public List<GrantRow> grants(UserContext me, @PathVariable long id) {
		return spaces.grants(me, id);
	}

	@PostMapping("/api/dms/spaces/{id}/grants")
	public ResponseEntity<GrantRow> addGrant(UserContext me, @PathVariable long id,
			@Valid @RequestBody GrantCreateRequest request) {
		GrantResult result = spaces.addGrant(me, id, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.row());
	}

	@PutMapping("/api/dms/spaces/{id}/grants/{gid}")
	public GrantRow updateGrant(UserContext me, @PathVariable long id, @PathVariable long gid,
			@Valid @RequestBody GrantUpdateRequest request) {
		return spaces.updateGrant(me, id, gid, request);
	}

	@DeleteMapping("/api/dms/spaces/{id}/grants/{gid}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeGrant(UserContext me, @PathVariable long id, @PathVariable long gid) {
		spaces.removeGrant(me, id, gid);
	}

	@GetMapping("/api/dms/spaces/{id}/items")
	public FolderContent content(UserContext me, @PathVariable long id, @RequestParam(required = false) Long folderId) {
		return items.content(me, id, folderId);
	}

	@GetMapping("/api/dms/spaces/{id}/folders")
	public List<FolderNode> folders(UserContext me, @PathVariable long id) {
		return items.folderNodes(me, id);
	}

	@PostMapping("/api/dms/spaces/{id}/folders")
	@ResponseStatus(HttpStatus.CREATED)
	public FolderRow createFolder(UserContext me, @PathVariable long id, @RequestBody FolderCreateRequest request) {
		return items.createFolder(me, id, request);
	}

	@PutMapping("/api/dms/folders/{id}")
	public FolderRow renameFolder(UserContext me, @PathVariable long id, @RequestBody RenameRequest request) {
		return items.renameFolder(me, id, request);
	}

	@PostMapping("/api/dms/folders/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void moveFolder(UserContext me, @PathVariable long id, @RequestBody MoveRequest request) {
		items.moveFolder(me, id, request);
	}

	@DeleteMapping("/api/dms/folders/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteFolder(UserContext me, @PathVariable long id) {
		items.trashFolder(me, id);
	}

	@PostMapping("/api/dms/spaces/{id}/files")
	public ResponseEntity<FileRow> saveFile(UserContext me, @PathVariable long id,
			@Valid @RequestBody SaveFileRequest request) {
		FileSaveResult result = items.saveFile(me, id, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.row());
	}

	@PutMapping("/api/dms/files/{id}")
	public FileRow renameFile(UserContext me, @PathVariable long id, @RequestBody RenameRequest request) {
		return items.renameFile(me, id, request);
	}

	@PostMapping("/api/dms/files/{id}/move")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void moveFile(UserContext me, @PathVariable long id, @RequestBody MoveRequest request) {
		items.moveFile(me, id, request);
	}

	@DeleteMapping("/api/dms/files/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteFile(UserContext me, @PathVariable long id) {
		items.trashFile(me, id);
	}

	@GetMapping("/api/dms/files/{id}/content")
	public ResponseEntity<Resource> fileContent(UserContext me, @PathVariable long id,
			@RequestParam(defaultValue = "false") boolean download) {
		DmsItemService.FileContent content = items.content(me, id, download);
		return storage.serve(content.file(), content.inline());
	}

	@GetMapping("/api/dms/files/{id}/locate")
	public LocateResponse locate(UserContext me, @PathVariable long id) {
		return items.locate(me, id);
	}

	@GetMapping("/api/dms/spaces/{id}/trash")
	public List<TrashRow> trash(UserContext me, @PathVariable long id) {
		return items.trash(me, id);
	}

	@PostMapping("/api/dms/trash/{type}/{itemId}/restore")
	public RestoreResult restore(UserContext me, @PathVariable String type, @PathVariable long itemId) {
		return items.restore(me, type, itemId);
	}

	@DeleteMapping("/api/dms/trash/{type}/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purge(UserContext me, @PathVariable String type, @PathVariable long itemId) {
		items.purge(me, type, itemId);
	}

	@DeleteMapping("/api/dms/spaces/{id}/trash")
	public PurgeResponse emptyTrash(UserContext me, @PathVariable long id) {
		return items.emptyTrash(me, id);
	}

	@GetMapping("/api/dms/recent")
	public List<SearchResult> recent(UserContext me) {
		return items.recentFiles(me);
	}

	@GetMapping("/api/dms/search")
	public List<SearchResult> search(UserContext me, @RequestParam(required = false, defaultValue = "") String q) {
		return items.search(me, q);
	}

}
