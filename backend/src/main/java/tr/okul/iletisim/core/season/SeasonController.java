package tr.okul.iletisim.core.season;

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
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.core.directory.DirectoryDtos.DirUser;
import tr.okul.iletisim.core.season.SeasonDtos.ActivationPreview;
import tr.okul.iletisim.core.season.SeasonDtos.AddMembershipRequest;
import tr.okul.iletisim.core.season.SeasonDtos.BatchRequest;
import tr.okul.iletisim.core.season.SeasonDtos.BatchResponse;
import tr.okul.iletisim.core.season.SeasonDtos.BulkDeleteRequest;
import tr.okul.iletisim.core.season.SeasonDtos.BulkDeleteResponse;
import tr.okul.iletisim.core.season.SeasonDtos.BulkUpdateRequest;
import tr.okul.iletisim.core.season.SeasonDtos.BulkUpdateResponse;
import tr.okul.iletisim.core.season.SeasonDtos.CopyRequest;
import tr.okul.iletisim.core.season.SeasonDtos.CopyResponse;
import tr.okul.iletisim.core.season.SeasonDtos.MembershipRow;
import tr.okul.iletisim.core.season.SeasonDtos.SeasonRequest;
import tr.okul.iletisim.core.season.SeasonDtos.SeasonRow;
import tr.okul.iletisim.core.user.UserDtos.MembershipInput;

@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/admin/seasons")
public class SeasonController {

	private final SeasonService service;
	private final SeasonQueries queries;
	private final MembershipService membershipService;

	public SeasonController(SeasonService service, SeasonQueries queries, MembershipService membershipService) {
		this.service = service;
		this.queries = queries;
		this.membershipService = membershipService;
	}

	@GetMapping
	public List<SeasonRow> list() {
		return queries.seasons();
	}

	@GetMapping("/{id}")
	public SeasonRow get(@PathVariable long id) {
		return queries.season(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SeasonRow create(UserContext me, @Valid @RequestBody SeasonRequest request) {
		return service.create(me, request);
	}

	@PutMapping("/{id}")
	public SeasonRow update(UserContext me, @PathVariable long id, @Valid @RequestBody SeasonRequest request) {
		return service.update(me, id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(UserContext me, @PathVariable long id) {
		service.delete(me, id);
	}

	@GetMapping("/{id}/memberships")
	public PageResponse<MembershipRow> memberships(@PathVariable long id, @RequestParam(defaultValue = "") String q,
			@RequestParam(required = false) Long branchId, @RequestParam(required = false) String filter,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		membershipService.findSeason(id);
		return queries.memberships(id, q, branchId, filter, page, size);
	}

	@GetMapping("/{id}/unassigned-users")
	public List<DirUser> unassigned(@PathVariable long id, @RequestParam(defaultValue = "") String q) {
		membershipService.findSeason(id);
		return queries.unassigned(id, q);
	}

	@PostMapping("/{id}/memberships")
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipRow addMembership(UserContext me, @PathVariable long id,
			@Valid @RequestBody AddMembershipRequest request) {
		return service.addMembership(me, id, request);
	}

	@PutMapping("/{id}/memberships/{mid}")
	public MembershipRow updateMembership(UserContext me, @PathVariable long id, @PathVariable long mid,
			@Valid @RequestBody MembershipInput request) {
		return service.updateMembership(me, id, mid, request);
	}

	@DeleteMapping("/{id}/memberships/{mid}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMembership(UserContext me, @PathVariable long id, @PathVariable long mid) {
		service.removeMembership(me, id, mid);
	}

	@PostMapping("/{id}/memberships/batch")
	public BatchResponse batch(UserContext me, @PathVariable long id, @Valid @RequestBody BatchRequest request) {
		return service.batch(me, id, request);
	}

	@PostMapping("/{id}/memberships/bulk-update")
	public BulkUpdateResponse bulkUpdate(UserContext me, @PathVariable long id,
			@Valid @RequestBody BulkUpdateRequest request) {
		return service.bulkUpdate(me, id, request);
	}

	@PostMapping("/{id}/memberships/bulk-delete")
	public BulkDeleteResponse bulkDelete(UserContext me, @PathVariable long id,
			@Valid @RequestBody BulkDeleteRequest request) {
		return service.bulkDelete(me, id, request);
	}

	@PostMapping("/{id}/memberships/copy")
	public CopyResponse copy(UserContext me, @PathVariable long id, @Valid @RequestBody CopyRequest request) {
		return service.copy(me, id, request);
	}

	@GetMapping("/{id}/activation-preview")
	public ActivationPreview preview(@PathVariable long id) {
		return service.preview(id);
	}

	@PostMapping("/{id}/activate")
	public SeasonRow activate(UserContext me, @PathVariable long id) {
		return service.activate(me, id);
	}

}
