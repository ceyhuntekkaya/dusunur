package tr.okul.iletisim.core.season;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.SessionService;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.branch.BranchRepository;
import tr.okul.iletisim.core.spi.MembershipRemovalHook;
import tr.okul.iletisim.core.user.AppUser;
import tr.okul.iletisim.core.user.AppUserRepository;
import tr.okul.iletisim.core.user.UserDtos.MembershipInput;
import tr.okul.iletisim.core.user.UserStatus;

@Service
public class MembershipService {

	private final SeasonRepository seasons;
	private final SeasonMembershipRepository memberships;
	private final AppUserRepository users;
	private final BranchRepository branches;
	private final JdbcClient jdbc;
	private final SessionService sessions;
	private final ObjectProvider<MembershipRemovalHook> removalHooks;

	public MembershipService(SeasonRepository seasons, SeasonMembershipRepository memberships, AppUserRepository users,
			BranchRepository branches, JdbcClient jdbc, SessionService sessions,
			ObjectProvider<MembershipRemovalHook> removalHooks) {
		this.seasons = seasons;
		this.memberships = memberships;
		this.users = users;
		this.branches = branches;
		this.jdbc = jdbc;
		this.sessions = sessions;
		this.removalHooks = removalHooks;
	}

	public Season findSeason(long id) {
		return seasons.findById(id).orElseThrow(() -> new BusinessException(CoreErrorCode.SEASON_NOT_FOUND));
	}

	public Season findWritableSeason(long id) {
		Season s = findSeason(id);
		if (s.getStatus() == SeasonStatus.CLOSED) {
			throw new BusinessException(CoreErrorCode.SEASON_CLOSED_READ_ONLY);
		}
		return s;
	}

	public AppUser findUser(long id) {
		return users.findById(id).orElseThrow(() -> new BusinessException(CoreErrorCode.USER_NOT_FOUND));
	}

	public SeasonMembership findMembership(long seasonId, long membershipId) {
		return memberships.findById(membershipId).filter(m -> m.getSeasonId() == seasonId)
				.orElseThrow(() -> new BusinessException(CoreErrorCode.SEASON_MEMBERSHIP_NOT_FOUND));
	}

	@Transactional
	public SeasonMembership add(Season season, AppUser user, MembershipInput in, String fieldPrefix) {
		if (memberships.findBySeasonIdAndUserId(season.getId(), user.getId()).isPresent()) {
			throw new BusinessException(CoreErrorCode.SEASON_MEMBER_EXISTS);
		}
		validate(season, user, in, fieldPrefix);
		SeasonMembership m = new SeasonMembership();
		m.setSeasonId(season.getId());
		m.setUserId(user.getId());
		apply(m, in);
		return memberships.saveAndFlush(m);
	}

	@Transactional
	public SeasonMembership update(Season season, SeasonMembership m, AppUser user, MembershipInput in,
			String fieldPrefix) {
		validate(season, user, in, fieldPrefix);
		apply(m, in);
		return memberships.saveAndFlush(m);
	}

	@Transactional
	public SeasonMembership upsert(Season season, AppUser user, MembershipInput in, String fieldPrefix) {
		SeasonMembership existing = memberships.findBySeasonIdAndUserId(season.getId(), user.getId()).orElse(null);
		return existing == null ? add(season, user, in, fieldPrefix) : update(season, existing, user, in, fieldPrefix);
	}

	public void requireActiveBranch(long branchId, String field) {
		var b = branches.findById(branchId).orElse(null);
		if (b == null || !b.isActive()) {
			throw BusinessException.field(CoreErrorCode.BRANCH_INACTIVE, field);
		}
	}

	public void requireApprover(long seasonId, Long selfUserId, long approverId, String field) {
		if (selfUserId != null && selfUserId == approverId) {
			throw BusinessException.field(CoreErrorCode.SEASON_APPROVER_SELF, field);
		}
		if (!isActiveMember(seasonId, approverId)) {
			throw BusinessException.field(CoreErrorCode.SEASON_APPROVER_NOT_MEMBER, field);
		}
	}

	public boolean isActiveMember(long seasonId, long userId) {
		return jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN app_user u ON u.id = m.user_id
				WHERE m.season_id = :sid AND m.user_id = :uid AND u.status = 'ACTIVE'
				""").param("sid", seasonId).param("uid", userId).query(Integer.class).single() > 0;
	}

	public int approveeCount(long seasonId, long userId, Collection<Long> excludeUserIds) {
		if (excludeUserIds.isEmpty()) {
			return (int) memberships.countBySeasonIdAndPrintApproverId(seasonId, userId);
		}
		return jdbc.sql("""
				SELECT count(*) FROM season_membership
				WHERE season_id = :sid AND print_approver_id = :uid AND user_id NOT IN (:ex)
				""").param("sid", seasonId).param("uid", userId).param("ex", excludeUserIds).query(Integer.class)
				.single();
	}

	public List<String> removalBlockers(long seasonId, long userId) {
		List<String> all = new ArrayList<>();
		removalHooks.orderedStream().forEach(h -> all.addAll(h.blockers(seasonId, userId)));
		return all;
	}

	@Transactional
	public void remove(Season season, SeasonMembership m) {
		int approvees = approveeCount(season.getId(), m.getUserId(), List.of());
		if (approvees > 0) {
			throw new BusinessException(CoreErrorCode.SEASON_MEMBER_IS_APPROVER, approvees);
		}
		List<String> blockers = removalBlockers(season.getId(), m.getUserId());
		if (!blockers.isEmpty()) {
			throw BusinessException.details(CoreErrorCode.SEASON_MEMBER_REMOVAL_BLOCKED, blockers, new Object[0]);
		}
		delete(season, m);
	}

	@Transactional
	public void delete(Season season, SeasonMembership m) {
		memberships.delete(m);
		memberships.flush();
		if (season.getStatus() == SeasonStatus.ACTIVE) {
			sessions.invalidateAll(m.getUserId(), null);
		}
	}

	private void validate(Season season, AppUser user, MembershipInput in, String p) {
		if (user.getStatus() == UserStatus.PASSIVE) {
			throw new BusinessException(CoreErrorCode.SEASON_USER_PASSIVE);
		}
		requireActiveBranch(in.branchId(), p + "branchId");
		if (in.printApproverId() != null) {
			requireApprover(season.getId(), user.getId(), in.printApproverId(), p + "printApproverId");
		}
	}

	private static void apply(SeasonMembership m, MembershipInput in) {
		m.setBranchId(in.branchId());
		m.setTitle(in.title());
		m.setManager(in.manager());
		m.setPrintOperator(in.printOperator());
		m.setCanCreateDmsSpace(in.canCreateDmsSpace());
		m.setPrintApproverId(in.printApproverId());
	}

}
