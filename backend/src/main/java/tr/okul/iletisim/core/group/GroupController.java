package tr.okul.iletisim.core.group;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirGroup;
import tr.okul.iletisim.core.directory.DirectoryQueries;
import tr.okul.iletisim.core.group.GroupDtos.AddMembersRequest;
import tr.okul.iletisim.core.group.GroupDtos.GroupCreateRequest;
import tr.okul.iletisim.core.group.GroupDtos.GroupDetail;
import tr.okul.iletisim.core.group.GroupDtos.GroupRow;
import tr.okul.iletisim.core.group.GroupDtos.GroupUpdateRequest;

@RestController
public class GroupController {

	private final GroupService service;
	private final DirectoryQueries directory;

	public GroupController(GroupService service, DirectoryQueries directory) {
		this.service = service;
		this.directory = directory;
	}

	@GetMapping("/api/groups/mine")
	public List<DirGroup> mine(UserContext me) {
		return directory.myGroups(me.userId());
	}

	@GetMapping("/api/groups")
	public List<GroupRow> list(UserContext me, @RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "false") boolean archived) {
		return service.list(me, q, archived);
	}

	@GetMapping("/api/groups/{id}")
	public GroupDetail detail(UserContext me, @PathVariable long id) {
		return service.detail(me, id);
	}

	@PostMapping("/api/groups")
	@ResponseStatus(HttpStatus.CREATED)
	public GroupDetail create(UserContext me, @Valid @RequestBody GroupCreateRequest request) {
		return service.create(me, request);
	}

	@PutMapping("/api/groups/{id}")
	public GroupDetail update(UserContext me, @PathVariable long id, @Valid @RequestBody GroupUpdateRequest request) {
		return service.update(me, id, request);
	}

	@PostMapping("/api/groups/{id}/members")
	public GroupDetail addMembers(UserContext me, @PathVariable long id,
			@Valid @RequestBody AddMembersRequest request) {
		return service.addMembers(me, id, request.userIds());
	}

	@DeleteMapping("/api/groups/{id}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(UserContext me, @PathVariable long id, @PathVariable long userId) {
		service.removeMember(me, id, userId);
	}

	@PostMapping("/api/groups/{id}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archive(UserContext me, @PathVariable long id) {
		service.archive(me, id);
	}

	@PostMapping("/api/groups/{id}/unarchive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unarchive(UserContext me, @PathVariable long id) {
		service.unarchive(me, id);
	}

}
