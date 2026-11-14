package tr.okul.iletisim.core.season;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.branch.Branch;
import tr.okul.iletisim.core.branch.BranchRepository;
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
import tr.okul.iletisim.core.spi.SeasonActivationHook;
import tr.okul.iletisim.core.user.AppUser;
import tr.okul.iletisim.core.user.AppUserRepository;
import tr.okul.iletisim.core.user.UserDtos.MembershipInput;
import tr.okul.iletisim.core.user.UserStatus;

@Service
public class SeasonService {

	private static final Pattern NAME = Pattern.compile("^(\\d{4})-(\\d{4})$");

	private final SeasonRepository seasons;
	private final SeasonMembershipRepository memberships;
	private final AppUserRepository users;
	private final BranchRepository branches;
	private final MembershipService membershipService;
	private final SeasonQueries queries;
	private final AuditService audit;
	private final ObjectProvider<SeasonActivationHook> activationHooks;

	public SeasonService(SeasonRepository seasons, SeasonMembershipRepository memberships, AppUserRepository users,
			BranchRepository branches, MembershipService membershipService, SeasonQueries queries, AuditService audit,
			ObjectProvider<SeasonActivationHook> activationHooks) {
		this.seasons = seasons;
		this.memberships = memberships;
		this.users = users;
		this.branches = branches;
		this.membershipService = membershipService;
		this.queries = queries;
		this.audit = audit;
		this.activationHooks = activationHooks;
	}

	@Transactional
	public SeasonRow create(UserContext me, SeasonRequest req) {
		validateName(req.name());
		validateDates(req);
		if (seasons.existsByName(req.name())) {
			throw BusinessException.field(CoreErrorCode.SEASON_NAME_TAKEN, "name");
		}
		if (seasons.existsByNameGreaterThanAndIdNot(req.name(), -1L)) {
			throw BusinessException.field(CoreErrorCode.SEASON_NAME_NOT_NEWER, "name");
		}
		seasons.findByStatus(SeasonStatus.DRAFT).ifPresent(d -> {
			throw new BusinessException(CoreErrorCode.SEASON_DRAFT_EXISTS, d.getName());
		});
		Season s = new Season();
		s.setName(req.name());
		s.setStartDate(req.startDate());
		s.setEndDate(req.endDate());
		s.setStatus(SeasonStatus.DRAFT);
		s = seasons.saveAndFlush(s);
		audit.log(me.userId(), CoreAuditAction.SEASON_CREATED, "SEASON", s.getId(), Map.of("name", s.getName()));
		return queries.season(s.getId());
	}

	@Transactional
	public SeasonRow update(UserContext me, long id, SeasonRequest req) {
		Season s = membershipService.findSeason(id);
		if (s.getStatus() == SeasonStatus.CLOSED) {
			throw new BusinessException(CoreErrorCode.SEASON_CLOSED_READ_ONLY);
		}
		validateName(req.name());
		validateDates(req);
		if (seasons.existsByNameAndIdNot(req.name(), id)) {
			throw BusinessException.field(CoreErrorCode.SEASON_NAME_TAKEN, "name");
		}
		if (s.getStatus() == SeasonStatus.DRAFT && seasons.existsByNameGreaterThanAndIdNot(req.name(), id)) {
			throw BusinessException.field(CoreErrorCode.SEASON_NAME_NOT_NEWER, "name");
		}
		s.setName(req.name());
		s.setStartDate(req.startDate());
		s.setEndDate(req.endDate());
		seasons.saveAndFlush(s);
		audit.log(me.userId(), CoreAuditAction.SEASON_UPDATED, "SEASON", id, Map.of("name", s.getName()));
		return queries.season(id);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		Season s = membershipService.findSeason(id);
		if (s.getStatus() != SeasonStatus.DRAFT) {
			throw new BusinessException(CoreErrorCode.SEASON_NOT_DRAFT);
		}
		seasons.delete(s);
		seasons.flush();
		audit.log(me.userId(), CoreAuditAction.SEASON_DELETED, "SEASON", id, Map.of("name", s.getName()));
	}

	@Transactional
	public MembershipRow addMembership(UserContext me, long seasonId, AddMembershipRequest req) {
		Season s = membershipService.findWritableSeason(seasonId);
		AppUser user = membershipService.findUser(req.userId());
		SeasonMembership m = membershipService.add(s, user, req.toInput(), "");
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_ADDED, "SEASON", seasonId,
				Map.of("userId", user.getId(), "branchId", m.getBranchId()));
		return queries.membership(m.getId());
	}

	@Transactional
	public MembershipRow updateMembership(UserContext me, long seasonId, long membershipId, MembershipInput in) {
		Season s = membershipService.findWritableSeason(seasonId);
		SeasonMembership m = membershipService.findMembership(seasonId, membershipId);
		AppUser user = membershipService.findUser(m.getUserId());
		membershipService.update(s, m, user, in, "");
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_UPDATED, "SEASON", seasonId,
				Map.of("userId", user.getId()));
		return queries.membership(membershipId);
	}

	@Transactional
	public void removeMembership(UserContext me, long seasonId, long membershipId) {
		Season s = membershipService.findWritableSeason(seasonId);
		SeasonMembership m = membershipService.findMembership(seasonId, membershipId);
		membershipService.remove(s, m);
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_REMOVED, "SEASON", seasonId,
				Map.of("userId", m.getUserId()));
	}

	@Transactional
	public BatchResponse batch(UserContext me, long seasonId, BatchRequest req) {
		Season s = membershipService.findWritableSeason(seasonId);
		membershipService.requireActiveBranch(req.branchId(), "branchId");
		Set<Long> ids = new LinkedHashSet<>(req.userIds());
		Map<Long, AppUser> found = new HashMap<>();
		users.findAllById(ids).forEach(u -> found.put(u.getId(), u));
		if (found.size() != ids.size()) {
			throw new BusinessException(CoreErrorCode.USER_NOT_FOUND);
		}
		Set<Long> existing = new HashSet<>();
		memberships.findBySeasonId(seasonId).forEach(m -> existing.add(m.getUserId()));
		List<AppUser> toAdd = new ArrayList<>();
		List<String> passive = new ArrayList<>();
		for (Long id : ids) {
			if (existing.contains(id)) {
				continue;
			}
			AppUser u = found.get(id);
			if (u.getStatus() == UserStatus.PASSIVE) {
				passive.add(u.fullName());
			}
			toAdd.add(u);
		}
		if (!passive.isEmpty()) {
			throw BusinessException.details(CoreErrorCode.SEASON_USER_PASSIVE, passive, new Object[0]);
		}
		for (AppUser u : toAdd) {
			SeasonMembership m = new SeasonMembership();
			m.setSeasonId(s.getId());
			m.setUserId(u.getId());
			m.setBranchId(req.branchId());
			memberships.save(m);
		}
		memberships.flush();
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_ADDED, "SEASON", seasonId,
				Map.of("count", toAdd.size()));
		return new BatchResponse(toAdd.size());
	}

	@Transactional
	public BulkUpdateResponse bulkUpdate(UserContext me, long seasonId, BulkUpdateRequest req) {
		Season s = membershipService.findWritableSeason(seasonId);
		boolean clear = Boolean.TRUE.equals(req.clearApprover());
		if (req.branchId() == null && req.printApproverId() == null && !clear) {
			throw new BusinessException(CoreErrorCode.BULK_NOTHING_TO_UPDATE);
		}
		if (req.printApproverId() != null && clear) {
			throw new BusinessException(CoreErrorCode.BULK_CONFLICT);
		}
		if (req.branchId() != null) {
			membershipService.requireActiveBranch(req.branchId(), "branchId");
		}
		if (req.printApproverId() != null) {
			membershipService.requireApprover(seasonId, null, req.printApproverId(), "printApproverId");
		}
		List<SeasonMembership> rows = loadMemberships(seasonId, req.membershipIds());
		int skipped = 0;
		for (SeasonMembership m : rows) {
			if (req.branchId() != null) {
				m.setBranchId(req.branchId());
			}
			if (clear) {
				m.setPrintApproverId(null);
			}
			else if (req.printApproverId() != null) {
				if (m.getUserId().equals(req.printApproverId())) {
					skipped++;
				}
				else {
					m.setPrintApproverId(req.printApproverId());
				}
			}
		}
		memberships.saveAllAndFlush(rows);
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_UPDATED, "SEASON", s.getId(),
				Map.of("count", rows.size()));
		return new BulkUpdateResponse(rows.size(), skipped);
	}

	@Transactional
	public BulkDeleteResponse bulkDelete(UserContext me, long seasonId, BulkDeleteRequest req) {
		Season s = membershipService.findWritableSeason(seasonId);
		List<SeasonMembership> rows = loadMemberships(seasonId, req.membershipIds());
		Set<Long> removing = new HashSet<>();
		rows.forEach(m -> removing.add(m.getUserId()));
		Map<Long, AppUser> userMap = new HashMap<>();
		users.findAllById(removing).forEach(u -> userMap.put(u.getId(), u));
		List<String> problems = new ArrayList<>();
		for (SeasonMembership m : rows) {
			String name = userMap.get(m.getUserId()).fullName();
			int approvees = membershipService.approveeCount(seasonId, m.getUserId(), removing);
			if (approvees > 0) {
				problems.add(name + ": Bu kişi bu yıl " + approvees + " personelin fotokopi onaylayıcısı.");
			}
			for (String b : membershipService.removalBlockers(seasonId, m.getUserId())) {
				problems.add(name + ": " + b);
			}
		}
		if (!problems.isEmpty()) {
			throw BusinessException.details(CoreErrorCode.SEASON_MEMBER_REMOVAL_BLOCKED, problems, new Object[0]);
		}
		for (SeasonMembership m : rows) {
			membershipService.delete(s, m);
		}
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIP_REMOVED, "SEASON", seasonId,
				Map.of("count", rows.size()));
		return new BulkDeleteResponse(rows.size());
	}

	@Transactional
	public CopyResponse copy(UserContext me, long seasonId, CopyRequest req) {
		Season target = membershipService.findWritableSeason(seasonId);
		if (req.sourceSeasonId() == seasonId) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "sourceSeasonId");
		}
		membershipService.findSeason(req.sourceSeasonId());
		Set<Long> wanted = new LinkedHashSet<>(req.userIds());
		Set<Long> existing = new HashSet<>();
		memberships.findBySeasonId(seasonId).forEach(m -> existing.add(m.getUserId()));
		Map<Long, AppUser> userMap = new HashMap<>();
		users.findAllById(wanted).forEach(u -> userMap.put(u.getId(), u));
		Map<Long, Branch> branchMap = new HashMap<>();
		branches.findAll().forEach(b -> branchMap.put(b.getId(), b));

		List<SeasonMembership> sources = new ArrayList<>();
		int skippedExisting = 0;
		for (Long uid : wanted) {
			if (existing.contains(uid)) {
				skippedExisting++;
				continue;
			}
			SeasonMembership src = memberships.findBySeasonIdAndUserId(req.sourceSeasonId(), uid)
					.orElseThrow(() -> new BusinessException(CoreErrorCode.SEASON_MEMBERSHIP_NOT_FOUND));
			sources.add(src);
		}
		List<String> branchProblems = new ArrayList<>();
		List<String> passive = new ArrayList<>();
		for (SeasonMembership src : sources) {
			AppUser u = userMap.get(src.getUserId());
			Branch b = branchMap.get(src.getBranchId());
			if (b == null || !b.isActive()) {
				branchProblems.add(u.fullName() + ": şube " + (b == null ? "" : b.getName()) + " pasif");
			}
			if (u.getStatus() == UserStatus.PASSIVE) {
				passive.add(u.fullName());
			}
		}
		if (!branchProblems.isEmpty()) {
			throw BusinessException.details(CoreErrorCode.SEASON_COPY_BRANCH_INACTIVE, branchProblems, new Object[0]);
		}
		if (!passive.isEmpty()) {
			throw BusinessException.details(CoreErrorCode.SEASON_USER_PASSIVE, passive, new Object[0]);
		}
		Set<Long> copySet = new HashSet<>();
		sources.forEach(m -> copySet.add(m.getUserId()));
		int cleared = 0;
		for (SeasonMembership src : sources) {
			SeasonMembership m = new SeasonMembership();
			m.setSeasonId(target.getId());
			m.setUserId(src.getUserId());
			m.setBranchId(src.getBranchId());
			m.setTitle(src.getTitle());
			m.setManager(src.isManager());
			m.setPrintOperator(src.isPrintOperator());
			m.setCanCreateDmsSpace(src.isCanCreateDmsSpace());
			Long approver = src.getPrintApproverId();
			if (approver != null) {
				boolean keep = approver != src.getUserId().longValue()
						&& (copySet.contains(approver) || existing.contains(approver))
						&& approverActive(approver, userMap);
				if (keep) {
					m.setPrintApproverId(approver);
				}
				else {
					cleared++;
				}
			}
			memberships.save(m);
		}
		memberships.flush();
		audit.log(me.userId(), CoreAuditAction.SEASON_MEMBERSHIPS_COPIED, "SEASON", seasonId,
				Map.of("count", sources.size(), "sourceSeasonId", req.sourceSeasonId()));
		return new CopyResponse(sources.size(), skippedExisting, cleared);
	}

	@Transactional(readOnly = true)
	public ActivationPreview preview(long id) {
		Season s = membershipService.findSeason(id);
		return buildPreview(s);
	}

	@Transactional
	public SeasonRow activate(UserContext me, long id) {
		queries.lockAllSeasons();
		Season s = membershipService.findSeason(id);
		ActivationPreview preview = buildPreview(s);
		if (!preview.canActivate()) {
			throw BusinessException.details(CoreErrorCode.SEASON_ACTIVATION_BLOCKED, preview.blockers(), new Object[0]);
		}
		Season old = seasons.findByStatus(SeasonStatus.ACTIVE).orElse(null);
		OffsetDateTime now = OffsetDateTime.now();
		if (old != null) {
			old.setStatus(SeasonStatus.CLOSED);
			old.setClosedAt(now);
			seasons.saveAndFlush(old);
		}
		s.setStatus(SeasonStatus.ACTIVE);
		s.setActivatedAt(now);
		seasons.saveAndFlush(s);
		if (old != null) {
			activationHooks.orderedStream().forEach(h -> h.onActivated(old.getId(), s.getId()));
		}
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("name", s.getName());
		if (old != null) {
			detail.put("closedSeasonId", old.getId());
		}
		audit.log(me.userId(), CoreAuditAction.SEASON_ACTIVATED, "SEASON", id, detail);
		return queries.season(id);
	}

	private ActivationPreview buildPreview(Season s) {
		List<String> blockers = new ArrayList<>();
		if (s.getStatus() != SeasonStatus.DRAFT) {
			blockers.add("Yalnızca hazırlık aşamasındaki yıl aktifleştirilebilir.");
		}
		if (memberships.countBySeasonId(s.getId()) == 0) {
			blockers.add("Bu yıla hiç personel atanmamış.");
		}
		List<String> items = new ArrayList<>();
		Season old = seasons.findByStatus(SeasonStatus.ACTIVE).filter(o -> !o.getId().equals(s.getId())).orElse(null);
		if (old != null) {
			items.add(old.getName() + " yılı kapanacak; o yılın içerikleri yalnızca görüntülenebilir olacak.");
		}
		items.add(s.getName() + " yılına atanmış " + queries.activeMemberCount(s.getId())
				+ " personel giriş yapabilecek.");
		if (old != null) {
			int dropped = queries.droppedMemberCount(old.getId(), s.getId());
			if (dropped > 0) {
				items.add(old.getName() + " yılında olup " + s.getName() + " yılına atanmamış " + dropped
						+ " personel artık giriş yapamayacak.");
			}
		}
		int noApprover = queries.withoutApproverCount(s.getId());
		if (noApprover > 0) {
			items.add("Fotokopi onaylayıcısı olmayan " + noApprover
					+ " personelin talepleri onaysız olarak fotokopi birimine düşecek.");
		}
		if (old != null) {
			activationHooks.orderedStream().forEach(h -> items.addAll(h.preview(old.getId())));
		}
		return new ActivationPreview(blockers.isEmpty(), blockers, items);
	}

	private boolean approverActive(long approverId, Map<Long, AppUser> loaded) {
		AppUser u = loaded.get(approverId);
		if (u == null) {
			u = users.findById(approverId).orElse(null);
		}
		return u != null && u.getStatus() == UserStatus.ACTIVE;
	}

	private List<SeasonMembership> loadMemberships(long seasonId, List<Long> ids) {
		Set<Long> unique = new LinkedHashSet<>(ids);
		List<SeasonMembership> rows = memberships.findAllById(unique);
		if (rows.size() != unique.size() || rows.stream().anyMatch(m -> m.getSeasonId() != seasonId)) {
			throw new BusinessException(CoreErrorCode.SEASON_MEMBERSHIP_NOT_FOUND);
		}
		return rows;
	}

	private void validateName(String name) {
		Matcher m = name == null ? null : NAME.matcher(name);
		if (m == null || !m.matches() || Integer.parseInt(m.group(2)) != Integer.parseInt(m.group(1)) + 1) {
			throw BusinessException.field(CoreErrorCode.SEASON_NAME_INVALID, "name");
		}
	}

	private void validateDates(SeasonRequest req) {
		if (!req.endDate().isAfter(req.startDate())) {
			throw BusinessException.field(CoreErrorCode.SEASON_DATES_INVALID, "endDate");
		}
	}

}
