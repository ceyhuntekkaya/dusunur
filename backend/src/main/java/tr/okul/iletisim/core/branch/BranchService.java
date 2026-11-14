package tr.okul.iletisim.core.branch;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.branch.BranchDtos.BranchRequest;
import tr.okul.iletisim.core.branch.BranchDtos.BranchRow;

@Service
public class BranchService {

	private final BranchRepository branches;
	private final BranchQueries queries;
	private final AuditService audit;

	public BranchService(BranchRepository branches, BranchQueries queries, AuditService audit) {
		this.branches = branches;
		this.queries = queries;
		this.audit = audit;
	}

	@Transactional
	public BranchRow create(UserContext me, BranchRequest req) {
		requireNameFree(req.name(), -1L);
		Branch b = new Branch();
		b.setName(req.name());
		b = branches.saveAndFlush(b);
		audit.log(me.userId(), CoreAuditAction.BRANCH_CREATED, "BRANCH", b.getId(), Map.of("name", b.getName()));
		return queries.row(b.getId());
	}

	@Transactional
	public BranchRow update(UserContext me, long id, BranchRequest req) {
		Branch b = find(id);
		requireNameFree(req.name(), id);
		b.setName(req.name());
		branches.saveAndFlush(b);
		audit.log(me.userId(), CoreAuditAction.BRANCH_UPDATED, "BRANCH", id, Map.of("name", b.getName()));
		return queries.row(id);
	}

	@Transactional
	public void deactivate(UserContext me, long id) {
		Branch b = find(id);
		if (!b.isActive()) {
			return;
		}
		int inUse = queries.blockingMemberCount(id);
		if (inUse > 0) {
			throw new BusinessException(CoreErrorCode.BRANCH_IN_USE, inUse);
		}
		if (branches.countByActiveTrue() <= 1) {
			throw new BusinessException(CoreErrorCode.BRANCH_LAST_ACTIVE);
		}
		b.setActive(false);
		branches.save(b);
		audit.log(me.userId(), CoreAuditAction.BRANCH_DEACTIVATED, "BRANCH", id, Map.of("name", b.getName()));
	}

	@Transactional
	public void activate(UserContext me, long id) {
		Branch b = find(id);
		if (b.isActive()) {
			return;
		}
		b.setActive(true);
		branches.save(b);
		audit.log(me.userId(), CoreAuditAction.BRANCH_ACTIVATED, "BRANCH", id, Map.of("name", b.getName()));
	}

	private Branch find(long id) {
		return branches.findById(id).orElseThrow(() -> new BusinessException(CoreErrorCode.BRANCH_NOT_FOUND));
	}

	private void requireNameFree(String name, long exceptId) {
		if (branches.existsByNameKeyAndIdNot(TextNormalizer.fold(name), exceptId)) {
			throw BusinessException.field(CoreErrorCode.BRANCH_NAME_TAKEN, "name");
		}
	}

}
