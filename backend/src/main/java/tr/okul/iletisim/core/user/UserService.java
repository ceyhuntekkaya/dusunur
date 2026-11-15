package tr.okul.iletisim.core.user;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.mail.MailService;
import tr.okul.iletisim.common.notify.PushSubscriptionRepository;
import tr.okul.iletisim.common.security.SessionService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.PhoneNormalizer;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.auth.AuthTokenService;
import tr.okul.iletisim.core.auth.TokenPurpose;
import tr.okul.iletisim.core.group.StaffGroupMember;
import tr.okul.iletisim.core.group.StaffGroupMemberRepository;
import tr.okul.iletisim.core.season.MembershipService;
import tr.okul.iletisim.core.season.Season;
import tr.okul.iletisim.core.season.SeasonMembership;
import tr.okul.iletisim.core.season.SeasonMembershipRepository;
import tr.okul.iletisim.core.season.SeasonRepository;
import tr.okul.iletisim.core.season.SeasonStatus;
import tr.okul.iletisim.core.spi.GroupMembersRemovedEvent;
import tr.okul.iletisim.core.spi.ImpactItem;
import tr.okul.iletisim.core.spi.UserDeactivatedEvent;
import tr.okul.iletisim.core.spi.UserDeactivationHook;
import tr.okul.iletisim.core.user.UserDtos.AdminUserRow;
import tr.okul.iletisim.core.user.UserDtos.CreateUserResponse;
import tr.okul.iletisim.core.user.UserDtos.DeactivationCheck;
import tr.okul.iletisim.core.user.UserDtos.MembershipInput;
import tr.okul.iletisim.core.user.UserDtos.UserInput;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final AppUserRepository users;
	private final SeasonRepository seasons;
	private final SeasonMembershipRepository memberships;
	private final MembershipService membershipService;
	private final UserQueries queries;
	private final AuthTokenService tokens;
	private final MailService mail;
	private final AppProperties app;
	private final AuditService audit;
	private final SessionService sessions;
	private final StaffGroupMemberRepository groupMembers;
	private final PushSubscriptionRepository pushSubscriptions;
	private final ApplicationEventPublisher events;
	private final ObjectProvider<UserDeactivationHook> hooks;

	public UserService(AppUserRepository users, SeasonRepository seasons, SeasonMembershipRepository memberships,
			MembershipService membershipService, UserQueries queries, AuthTokenService tokens, MailService mail,
			AppProperties app, AuditService audit, SessionService sessions, StaffGroupMemberRepository groupMembers,
			PushSubscriptionRepository pushSubscriptions, ApplicationEventPublisher events,
			ObjectProvider<UserDeactivationHook> hooks) {
		this.users = users;
		this.seasons = seasons;
		this.memberships = memberships;
		this.membershipService = membershipService;
		this.queries = queries;
		this.tokens = tokens;
		this.mail = mail;
		this.app = app;
		this.audit = audit;
		this.sessions = sessions;
		this.groupMembers = groupMembers;
		this.pushSubscriptions = pushSubscriptions;
		this.events = events;
		this.hooks = hooks;
	}

	@Transactional
	public CreateUserResponse create(UserContext me, UserInput in) {
		if (users.existsByEmail(in.email())) {
			throw BusinessException.field(CoreErrorCode.USER_EMAIL_TAKEN, "email");
		}
		Season active = null;
		if (in.membership() != null) {
			active = activeSeason();
			membershipService.requireActiveBranch(in.membership().branchId(), "membership.branchId");
			if (in.membership().printApproverId() != null) {
				membershipService.requireApprover(active.getId(), null, in.membership().printApproverId(),
						"membership.printApproverId");
			}
		}
		AppUser u = new AppUser();
		fill(u, in);
		u = users.saveAndFlush(u);
		if (in.membership() != null) {
			membershipService.add(active, u, in.membership(), "membership.");
		}
		String raw = tokens.issue(u.getId(), TokenPurpose.SETUP, Duration.ofHours(app.auth().setupTokenHours()));
		boolean sent = mail.sendSetupLink(u.getEmail(), u.getFirstName(), tokens.link(raw), false);
		if (!sent) {
			log.warn("Kurulum e-postası gönderilemedi: {}", u.getEmail());
		}
		audit.log(me.userId(), CoreAuditAction.USER_CREATED, "USER", u.getId(), Map.of("email", u.getEmail()));
		return new CreateUserResponse(queries.row(u.getId()), sent);
	}

	@Transactional
	public AdminUserRow update(UserContext me, long id, UserInput in) {
		AppUser u = find(id);
		if (users.existsByEmailAndIdNot(in.email(), id)) {
			throw BusinessException.field(CoreErrorCode.USER_EMAIL_TAKEN, "email");
		}
		if (u.isSystemAdmin() && !in.systemAdmin()) {
			if (id == me.userId()) {
				throw new BusinessException(CoreErrorCode.USER_CANNOT_REMOVE_OWN_ADMIN);
			}
			if (u.getStatus() == UserStatus.ACTIVE
					&& users.countBySystemAdminTrueAndStatus(UserStatus.ACTIVE) <= 1) {
				throw new BusinessException(CoreErrorCode.USER_LAST_ADMIN);
			}
		}
		List<String> changed = new ArrayList<>();
		String phone = PhoneNormalizer.normalize(in.phone());
		track(changed, "firstName", u.getFirstName(), in.firstName());
		track(changed, "lastName", u.getLastName(), in.lastName());
		track(changed, "email", u.getEmail(), in.email());
		track(changed, "phone", u.getPhone(), phone);
		track(changed, "discipline", u.getDiscipline(), in.discipline());
		track(changed, "systemAdmin", u.isSystemAdmin(), in.systemAdmin());
		boolean emailChanged = !Objects.equals(u.getEmail(), in.email());
		if (in.membership() != null && u.getStatus() != UserStatus.PASSIVE) {
			Season active = activeSeason();
			SeasonMembership before = memberships.findBySeasonIdAndUserId(active.getId(), id).orElse(null);
			if (membershipChanged(before, in.membership())) {
				changed.add("membership");
			}
			membershipService.upsert(active, u, in.membership(), "membership.");
		}
		fill(u, in);
		users.saveAndFlush(u);
		if (emailChanged) {
			tokens.invalidateOpen(id);
		}
		audit.log(me.userId(), CoreAuditAction.USER_UPDATED, "USER", id, Map.of("changed", changed));
		return queries.row(id);
	}

	@Transactional
	public void sendSetupLink(UserContext me, long id) {
		AppUser u = find(id);
		if (u.getStatus() == UserStatus.PASSIVE) {
			throw new BusinessException(CoreErrorCode.USER_PASSIVE);
		}
		tokens.invalidateOpen(id);
		String raw = tokens.issue(id, TokenPurpose.SETUP, Duration.ofHours(app.auth().setupTokenHours()));
		if (!mail.sendSetupLink(u.getEmail(), u.getFirstName(), tokens.link(raw), u.getPasswordHash() != null)) {
			throw new BusinessException(CoreErrorCode.MAIL_SEND_FAILED);
		}
		audit.log(me.userId(), CoreAuditAction.USER_SETUP_LINK_SENT, "USER", id, Map.of("email", u.getEmail()));
	}

	@Transactional(readOnly = true)
	public DeactivationCheck deactivationCheck(long id) {
		find(id);
		List<ImpactItem> items = impacts(id);
		return new DeactivationCheck(items, !items.isEmpty());
	}

	@Transactional
	public void deactivate(UserContext me, long id, Long successorUserId) {
		AppUser u = find(id);
		if (id == me.userId()) {
			throw new BusinessException(CoreErrorCode.USER_CANNOT_DEACTIVATE_SELF);
		}
		if (u.getStatus() == UserStatus.PASSIVE) {
			throw new BusinessException(CoreErrorCode.USER_ALREADY_PASSIVE);
		}
		if (u.isSystemAdmin() && users.countBySystemAdminTrueAndStatus(UserStatus.ACTIVE) <= 1) {
			throw new BusinessException(CoreErrorCode.USER_LAST_ADMIN);
		}
		List<ImpactItem> items = impacts(id);
		if (!items.isEmpty() && successorUserId == null) {
			String list = String.join(", ", items.stream().map(i -> i.count() + " " + i.label()).toList());
			throw BusinessException.field(CoreErrorCode.USER_SUCCESSOR_REQUIRED, "successorUserId", list);
		}
		if (successorUserId != null) {
			Season active = seasons.findByStatus(SeasonStatus.ACTIVE).orElse(null);
			AppUser successor = users.findById(successorUserId).orElse(null);
			if (successorUserId == id || successor == null || successor.getStatus() != UserStatus.ACTIVE
					|| active == null || !membershipService.isActiveMember(active.getId(), successorUserId)) {
				throw BusinessException.field(CoreErrorCode.USER_SUCCESSOR_INVALID, "successorUserId");
			}
		}
		u.setStatus(UserStatus.PASSIVE);
		users.saveAndFlush(u);
		hooks.orderedStream().forEach(h -> h.transfer(id, successorUserId));
		List<StaffGroupMember> groups = groupMembers.findByUserId(id);
		groupMembers.deleteByUserId(id);
		groupMembers.flush();
		for (StaffGroupMember g : groups) {
			events.publishEvent(new GroupMembersRemovedEvent(g.getGroupId(), List.of(id)));
		}
		pushSubscriptions.deleteByUserId(id);
		events.publishEvent(new UserDeactivatedEvent(id));
		sessions.invalidateAll(id, null);
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("successorUserId", successorUserId);
		audit.log(me.userId(), CoreAuditAction.USER_DEACTIVATED, "USER", id, detail);
	}

	@Transactional
	public void reactivate(UserContext me, long id) {
		AppUser u = find(id);
		if (u.getStatus() == UserStatus.ACTIVE) {
			throw new BusinessException(CoreErrorCode.USER_ALREADY_ACTIVE);
		}
		u.setStatus(UserStatus.ACTIVE);
		u.setFailedLoginCount(0);
		u.setLockedUntil(null);
		users.save(u);
		audit.log(me.userId(), CoreAuditAction.USER_REACTIVATED, "USER", id, Map.of("email", u.getEmail()));
	}

	private List<ImpactItem> impacts(long id) {
		List<ImpactItem> items = new ArrayList<>();
		hooks.orderedStream().forEach(h -> h.describe(id).stream().filter(i -> i.count() > 0).forEach(items::add));
		return items;
	}

	private Season activeSeason() {
		return seasons.findByStatus(SeasonStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.SEASON_NONE_ACTIVE));
	}

	private AppUser find(long id) {
		return users.findById(id).orElseThrow(() -> new BusinessException(CoreErrorCode.USER_NOT_FOUND));
	}

	private static void fill(AppUser u, UserInput in) {
		u.setFirstName(in.firstName());
		u.setLastName(in.lastName());
		u.setEmail(in.email());
		u.setPhone(PhoneNormalizer.normalize(in.phone()));
		u.setDiscipline(in.discipline());
		u.setSystemAdmin(in.systemAdmin());
	}

	private static void track(List<String> changed, String field, Object before, Object after) {
		if (!Objects.equals(before, after)) {
			changed.add(field);
		}
	}

	private static boolean membershipChanged(SeasonMembership m, MembershipInput in) {
		return m == null || !Objects.equals(m.getBranchId(), in.branchId()) || !Objects.equals(m.getTitle(), in.title())
				|| m.isManager() != in.manager() || m.isPrintOperator() != in.printOperator()
				|| m.isCanCreateDmsSpace() != in.canCreateDmsSpace()
				|| !Objects.equals(m.getPrintApproverId(), in.printApproverId());
	}

}
