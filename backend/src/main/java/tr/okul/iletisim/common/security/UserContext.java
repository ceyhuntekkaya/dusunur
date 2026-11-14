package tr.okul.iletisim.common.security;

import java.util.Set;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.core.CoreErrorCode;

public record UserContext(long userId, String firstName, String lastName, boolean systemAdmin,
		Long seasonId, String seasonName, Membership membership, Set<Long> groupIds) {

	public record Membership(long id, long branchId, String branchName, String title, boolean manager,
			boolean printOperator, boolean canCreateDmsSpace, Long printApproverId) {
	}

	public String fullName() {
		return firstName + " " + lastName;
	}

	public boolean isMember() {
		return membership != null;
	}

	public boolean isManager() {
		return membership != null && membership.manager();
	}

	public boolean canManageGroups() {
		return systemAdmin || isManager();
	}

	public boolean canCreateDmsSpace() {
		return membership != null && (membership.manager() || membership.canCreateDmsSpace());
	}

	public long requireMember() {
		if (membership == null || seasonId == null) {
			throw new BusinessException(CommonErrorCode.MEMBERSHIP_REQUIRED);
		}
		return seasonId;
	}

	public void requireManager() {
		if (!isManager()) {
			throw new BusinessException(CoreErrorCode.CORE_MANAGER_ONLY);
		}
	}

	public void requireWritable(long recordSeasonId) {
		if (seasonId == null || recordSeasonId != seasonId) {
			throw new BusinessException(CommonErrorCode.SEASON_READ_ONLY);
		}
	}

}
