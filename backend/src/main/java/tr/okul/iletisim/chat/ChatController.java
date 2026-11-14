package tr.okul.iletisim.chat;

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
import tr.okul.iletisim.chat.ChatDtos.AddMembersRequest;
import tr.okul.iletisim.chat.ChatDtos.ConversationDetail;
import tr.okul.iletisim.chat.ChatDtos.ConversationRow;
import tr.okul.iletisim.chat.ChatDtos.CreateGroupRequest;
import tr.okul.iletisim.chat.ChatDtos.DirectRequest;
import tr.okul.iletisim.chat.ChatDtos.MessageDto;
import tr.okul.iletisim.chat.ChatDtos.MuteRequest;
import tr.okul.iletisim.chat.ChatDtos.ReadRequest;
import tr.okul.iletisim.chat.ChatDtos.RenameRequest;
import tr.okul.iletisim.chat.ChatDtos.SendMessageRequest;
import tr.okul.iletisim.chat.ChatDtos.SendResult;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;

@RestController
public class ChatController {

	private final ChatConversationService conversations;
	private final ChatMessageService messages;
	private final FileStorageService files;

	public ChatController(ChatConversationService conversations, ChatMessageService messages,
			FileStorageService files) {
		this.conversations = conversations;
		this.messages = messages;
		this.files = files;
	}

	@GetMapping("/api/chat/conversations")
	public List<ConversationRow> list(UserContext me) {
		return conversations.list(me);
	}

	@PostMapping("/api/chat/direct")
	public ConversationRow direct(UserContext me, @Valid @RequestBody DirectRequest request) {
		return conversations.openDirect(me, request.userId());
	}

	@PostMapping("/api/chat/groups")
	@ResponseStatus(HttpStatus.CREATED)
	public ConversationRow createGroup(UserContext me, @Valid @RequestBody CreateGroupRequest request) {
		return conversations.createGroup(me, request);
	}

	@GetMapping("/api/chat/conversations/{id}")
	public ConversationDetail detail(UserContext me, @PathVariable long id) {
		return conversations.detail(me, id);
	}

	@PutMapping("/api/chat/conversations/{id}")
	public ConversationDetail rename(UserContext me, @PathVariable long id,
			@Valid @RequestBody RenameRequest request) {
		return conversations.rename(me, id, request);
	}

	@PostMapping("/api/chat/conversations/{id}/members")
	public ConversationDetail addMembers(UserContext me, @PathVariable long id,
			@Valid @RequestBody AddMembersRequest request) {
		return conversations.addMembers(me, id, request);
	}

	@DeleteMapping("/api/chat/conversations/{id}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(UserContext me, @PathVariable long id, @PathVariable long userId) {
		conversations.removeMember(me, id, userId);
	}

	@PostMapping("/api/chat/conversations/{id}/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(UserContext me, @PathVariable long id) {
		conversations.leave(me, id);
	}

	@PostMapping("/api/chat/conversations/{id}/mute")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void mute(UserContext me, @PathVariable long id, @RequestBody MuteRequest request) {
		conversations.mute(me, id, request.muted());
	}

	@GetMapping("/api/chat/conversations/{id}/messages")
	public CursorResponse<MessageDto> messages(UserContext me, @PathVariable long id,
			@RequestParam(required = false) Long before, @RequestParam(defaultValue = "50") int size) {
		return messages.list(me, id, before, size);
	}

	@PostMapping("/api/chat/conversations/{id}/messages")
	public ResponseEntity<MessageDto> send(UserContext me, @PathVariable long id,
			@Valid @RequestBody SendMessageRequest request) {
		SendResult result = messages.send(me, id, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.message());
	}

	@DeleteMapping("/api/chat/messages/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(UserContext me, @PathVariable long id) {
		messages.delete(me, id);
	}

	@PostMapping("/api/chat/conversations/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void read(UserContext me, @PathVariable long id, @Valid @RequestBody ReadRequest request) {
		messages.markRead(me, id, request.lastMessageId());
	}

	@GetMapping("/api/chat/conversations/{id}/files/{fileId}")
	public ResponseEntity<Resource> file(UserContext me, @PathVariable long id, @PathVariable UUID fileId,
			@RequestParam(defaultValue = "false") boolean download) {
		return files.serve(messages.file(me, id, fileId), !download);
	}

}
