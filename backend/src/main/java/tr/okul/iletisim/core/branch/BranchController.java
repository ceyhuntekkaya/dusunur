package tr.okul.iletisim.core.branch;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.branch.BranchDtos.BranchRequest;
import tr.okul.iletisim.core.branch.BranchDtos.BranchRow;
import tr.okul.iletisim.core.branch.BranchDtos.DirBranch;

@RestController
public class BranchController {

	private final BranchService service;
	private final BranchQueries queries;

	public BranchController(BranchService service, BranchQueries queries) {
		this.service = service;
		this.queries = queries;
	}

	@GetMapping("/api/branches")
	public List<DirBranch> activeBranches() {
		return queries.activeBranches();
	}

	@GetMapping("/api/admin/branches")
	public List<BranchRow> list() {
		return queries.rows();
	}

	@PostMapping("/api/admin/branches")
	@ResponseStatus(HttpStatus.CREATED)
	public BranchRow create(UserContext me, @Valid @RequestBody BranchRequest request) {
		return service.create(me, request);
	}

	@PutMapping("/api/admin/branches/{id}")
	public BranchRow update(UserContext me, @PathVariable long id, @Valid @RequestBody BranchRequest request) {
		return service.update(me, id, request);
	}

	@PostMapping("/api/admin/branches/{id}/deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(UserContext me, @PathVariable long id) {
		service.deactivate(me, id);
	}

	@PostMapping("/api/admin/branches/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activate(UserContext me, @PathVariable long id) {
		service.activate(me, id);
	}

}
