package tr.okul.iletisim.print;

import java.util.Objects;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.print.PrintDtos.PrintActions;

@Component
public class PrintAccess {

	public boolean isOwner(UserContext me, PrintRequest r) {
		return Objects.equals(r.getRequesterId(), me.userId());
	}

	public boolean isArchived(UserContext me, PrintRequest r) {
		return me.seasonId() == null || !Objects.equals(r.getSeasonId(), me.seasonId());
	}

	public boolean isOperator(UserContext me, PrintRequest r) {
		return me.isMember() && me.membership().printOperator()
				&& Objects.equals(r.getBranchId(), me.membership().branchId()) && !isArchived(me, r);
	}

	public void requireOperator(UserContext me) {
		if (!me.isMember() || !me.membership().printOperator()) {
			throw new BusinessException(PrintErrorCode.PRINT_OPERATOR_ONLY);
		}
	}

	public boolean isAssignedApprover(UserContext me, PrintRequest r) {
		return Objects.equals(r.getApproverId(), me.userId());
	}

	public boolean canView(UserContext me, PrintRequest r) {
		if (isOwner(me, r) || isAssignedApprover(me, r) || Objects.equals(r.getDecidedBy(), me.userId())) {
			return true;
		}
		return isOperator(me, r) && r.getStatus() != PrintStatus.PENDING_APPROVAL
				&& r.getStatus() != PrintStatus.REJECTED
				&& (r.getStatus() != PrintStatus.CANCELLED || r.getApproverId() == null || r.getDecidedBy() != null);
	}

	public boolean canAccessFile(UserContext me, PrintRequest r) {
		return canView(me, r) && !isArchived(me, r);
	}

	public PrintActions actions(UserContext me, PrintRequest r) {
		boolean archived = isArchived(me, r);
		PrintStatus s = r.getStatus();
		boolean owner = isOwner(me, r);
		boolean canCopy = owner
				&& (s == PrintStatus.REJECTED || s == PrintStatus.CANCELLED || s == PrintStatus.DELIVERED);
		if (archived) {
			return new PrintActions(false, canCopy, false, false, false, false, false, false);
		}
		boolean decide = isAssignedApprover(me, r) && s == PrintStatus.PENDING_APPROVAL;
		boolean operator = isOperator(me, r);
		boolean fileVisible = canView(me, r) && r.getFileId() != null && r.getFilePurgedAt() == null;
		return new PrintActions(
				owner && (s == PrintStatus.PENDING_APPROVAL || s == PrintStatus.APPROVED),
				canCopy, decide, decide,
				operator && s == PrintStatus.APPROVED,
				operator && (s == PrintStatus.APPROVED || s == PrintStatus.IN_PROGRESS),
				operator && s == PrintStatus.READY,
				fileVisible);
	}

}
