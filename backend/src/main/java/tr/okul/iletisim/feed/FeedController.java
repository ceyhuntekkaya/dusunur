package tr.okul.iletisim.feed;

import java.util.List;
import java.util.UUID;

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
import tr.okul.iletisim.common.web.CursorResponse;
import tr.okul.iletisim.feed.FeedDtos.CommentDto;
import tr.okul.iletisim.feed.FeedDtos.CommentRequest;
import tr.okul.iletisim.feed.FeedDtos.CreatePostRequest;
import tr.okul.iletisim.feed.FeedDtos.CreatePostResponse;
import tr.okul.iletisim.feed.FeedDtos.FeedSeason;
import tr.okul.iletisim.feed.FeedDtos.LikeResponse;
import tr.okul.iletisim.feed.FeedDtos.PostDto;
import tr.okul.iletisim.feed.FeedDtos.ReadStatusResponse;
import tr.okul.iletisim.feed.FeedDtos.RemindResponse;
import tr.okul.iletisim.feed.FeedDtos.UpdatePostRequest;

@RestController
public class FeedController {

	private final FeedService service;
	private final FeedInteractionService interactions;
	private final FileStorageService files;

	public FeedController(FeedService service, FeedInteractionService interactions, FileStorageService files) {
		this.service = service;
		this.interactions = interactions;
		this.files = files;
	}

	@GetMapping("/api/feed/seasons")
	public List<FeedSeason> seasons(UserContext me) {
		return service.seasons(me);
	}

	@GetMapping("/api/feed/posts")
	public CursorResponse<PostDto> list(UserContext me, @RequestParam(required = false) Long seasonId,
			@RequestParam(defaultValue = "all") String filter, @RequestParam(required = false) Long groupId,
			@RequestParam(required = false) Long before, @RequestParam(defaultValue = "10") int size) {
		return service.list(me, seasonId, filter, groupId, before, size);
	}

	@GetMapping("/api/feed/pinned")
	public List<PostDto> pinned(UserContext me) {
		return service.pinned(me);
	}

	@GetMapping("/api/feed/posts/{id}")
	public PostDto detail(UserContext me, @PathVariable long id) {
		return service.detail(me, id);
	}

	@GetMapping("/api/feed/posts/{id}/comments")
	public CursorResponse<CommentDto> comments(UserContext me, @PathVariable long id,
			@RequestParam(required = false) Long after, @RequestParam(defaultValue = "50") int size) {
		return service.comments(me, id, after, size);
	}

	@GetMapping("/api/feed/posts/{id}/files/{fileId}")
	public ResponseEntity<Resource> file(UserContext me, @PathVariable long id, @PathVariable UUID fileId,
			@RequestParam(defaultValue = "false") boolean download) {
		return files.serve(service.file(me, id, fileId), !download);
	}

	@PostMapping("/api/feed/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public CreatePostResponse create(UserContext me, @Valid @RequestBody CreatePostRequest request) {
		return service.create(me, request);
	}

	@PutMapping("/api/feed/posts/{id}")
	public PostDto update(UserContext me, @PathVariable long id, @Valid @RequestBody UpdatePostRequest request) {
		return service.update(me, id, request);
	}

	@DeleteMapping("/api/feed/posts/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(UserContext me, @PathVariable long id) {
		service.delete(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/pin")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void pin(UserContext me, @PathVariable long id) {
		service.pin(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/unpin")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unpin(UserContext me, @PathVariable long id) {
		service.unpin(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/like")
	public LikeResponse like(UserContext me, @PathVariable long id) {
		return interactions.like(me, id);
	}

	@DeleteMapping("/api/feed/posts/{id}/like")
	public LikeResponse unlike(UserContext me, @PathVariable long id) {
		return interactions.unlike(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void read(UserContext me, @PathVariable long id) {
		interactions.markRead(me, id);
	}

	@GetMapping("/api/feed/posts/{id}/reads")
	public ReadStatusResponse reads(UserContext me, @PathVariable long id) {
		return interactions.reads(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/remind")
	public RemindResponse remind(UserContext me, @PathVariable long id) {
		return interactions.remind(me, id);
	}

	@PostMapping("/api/feed/posts/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentDto addComment(UserContext me, @PathVariable long id, @Valid @RequestBody CommentRequest request) {
		return interactions.addComment(me, id, request);
	}

	@DeleteMapping("/api/feed/comments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(UserContext me, @PathVariable long id) {
		interactions.deleteComment(me, id);
	}

}
