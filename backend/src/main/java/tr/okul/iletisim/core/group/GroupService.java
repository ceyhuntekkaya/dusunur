package tr.okul.iletisim.core.group;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.audit.CoreAuditAction;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.group.GroupDtos.GroupCreateRequest;
import tr.okul.iletisim.core.group.GroupDtos.GroupDetail;
import tr.okul.iletisim.core.group.GroupDtos.GroupRow;
import tr.okul.iletisim.core.group.GroupDtos.GroupUpdateRequest;
import tr.okul.iletisim.core.spi.GroupArchivedEvent;
import tr.okul.iletisim.core.spi.GroupCreatedEvent;
import tr.okul.iletisim.core.spi.GroupMembersAddedEvent;
import tr.okul.iletisim.core.spi.GroupMembersRemovedEvent;
import tr.okul.iletisim.core.spi.GroupUpdatedEvent;

@Service
public class GroupService {

	private final StaffGroupRepository groups;
	private final StaffGroupMemberRepository members;
	private final GroupQueries queries;
	private final AuditService audit;
	private final ApplicationEventPublisher events;

	public GroupService(StaffGroupRepository groups, StaffGroupMemberRepository members, GroupQueries queries,
			AuditService audit, ApplicationEventPublisher events) {
		this.groups = groups;
		this.members = members;
		this.queries = queries;
		this.audit = audit;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public List<GroupRow> list(UserContext me, String q, boolean archived) {
		requireManage(me);
		return queries.list(q, archived);
	}

	@Transactional(readOnly = true)
	public GroupDetail detail(UserContext me, long id) {
		requireManage(me);
		return queries.detail(id);
	}

	@Transactional
	public GroupDetail create(UserContext me, GroupCreateRequest req) {
		requireManage(me);
		if (groups.existsByNameKey(TextNormalizer.fold(req.name()))) {
			throw BusinessException.field(CoreErrorCode.GROUP_NAME_TAKEN, "name");
		}
		Set<Long> memberIds = new LinkedHashSet<>(req.memberIds());
		requireActiveUsers(memberIds);
		StaffGroup g = new StaffGroup();
		g.setName(req.name());
		g.setDescription(req.description());
		g.setCreatedBy(me.userId());
		g = groups.saveAndFlush(g);
		for (Long uid : memberIds) {
			members.save(new StaffGroupMember(g.getId(), uid));
		}
		members.flush();
		audit.log(me.userId(), CoreAuditAction.GROUP_CREATED, "GROUP", g.getId(),
				Map.of("name", g.getName(), "memberCount", memberIds.size()));
		events.publishEvent(new GroupCreatedEvent(g.getId(), g.getName(), new ArrayList<>(memberIds)));
		return queries.detail(g.getId());
	}

	@Transactional
	public GroupDetail update(UserContext me, long id, GroupUpdateRequest req) {
		requireManage(me);
		StaffGroup g = findEditable(id);
		if (groups.existsByNameKeyAndIdNot(TextNormalizer.fold(req.name()), id)) {
			throw BusinessException.field(CoreErrorCode.GROUP_NAME_TAKEN, "name");
		}
		g.setName(req.name());
		g.setDescription(req.description());
		groups.saveAndFlush(g);
		audit.log(me.userId(), CoreAuditAction.GROUP_UPDATED, "GROUP", id, Map.of("name", g.getName()));
		events.publishEvent(new GroupUpdatedEvent(id, g.getName()));
		return queries.detail(id);
	}

	@Transactional
	public GroupDetail addMembers(UserContext me, long id, List<Long> userIds) {
		requireManage(me);
		findEditable(id);
		Set<Long> wanted = new LinkedHashSet<>(userIds);
		requireActiveUsers(wanted);
		Set<Long> existing = new LinkedHashSet<>();
		members.findByGroupId(id).forEach(m -> existing.add(m.getUserId()));
		List<Long> added = new ArrayList<>();
		for (Long uid : wanted) {
			if (!existing.contains(uid)) {
				members.save(new StaffGroupMember(id, uid));
				added.add(uid);
			}
		}
		members.flush();
		if (!added.isEmpty()) {
			audit.log(me.userId(), CoreAuditAction.GROUP_MEMBERS_ADDED, "GROUP", id, Map.of("userIds", added));
			events.publishEvent(new GroupMembersAddedEvent(id, added));
		}
		return queries.detail(id);
	}

	@Transactional
	public void removeMember(UserContext me, long id, long userId) {
		requireManage(me);
		findEditable(id);
		StaffGroupMemberId key = new StaffGroupMemberId(id, userId);
		if (members.existsById(key)) {
			members.deleteById(key);
			members.flush();
			audit.log(me.userId(), CoreAuditAction.GROUP_MEMBER_REMOVED, "GROUP", id, Map.of("userId", userId));
			events.publishEvent(new GroupMembersRemovedEvent(id, List.of(userId)));
		}
	}

	@Transactional
	public void archive(UserContext me, long id) {
		requireManage(me);
		StaffGroup g = find(id);
		if (g.isArchived()) {
			return;
		}
		g.setArchived(true);
		groups.saveAndFlush(g);
		audit.log(me.userId(), CoreAuditAction.GROUP_ARCHIVED, "GROUP", id, Map.of("name", g.getName()));
		events.publishEvent(new GroupArchivedEvent(id, true));
	}

	@Transactional
	public void unarchive(UserContext me, long id) {
		requireManage(me);
		StaffGroup g = find(id);
		if (!g.isArchived()) {
			return;
		}
		g.setArchived(false);
		groups.saveAndFlush(g);
		audit.log(me.userId(), CoreAuditAction.GROUP_UNARCHIVED, "GROUP", id, Map.of("name", g.getName()));
		events.publishEvent(new GroupArchivedEvent(id, false));
	}

	private void requireManage(UserContext me) {
		if (!me.canManageGroups()) {
			throw new BusinessException(CoreErrorCode.GROUP_MANAGE_FORBIDDEN);
		}
	}

	private StaffGroup find(long id) {
		return groups.findById(id).orElseThrow(() -> new BusinessException(CoreErrorCode.GROUP_NOT_FOUND));
	}

	private StaffGroup findEditable(long id) {
		StaffGroup g = find(id);
		if (g.isArchived()) {
			throw new BusinessException(CoreErrorCode.GROUP_ARCHIVED);
		}
		return g;
	}

	private void requireActiveUsers(Set<Long> ids) {
		if (queries.countActiveUsers(ids) != ids.size()) {
			throw new BusinessException(CoreErrorCode.GROUP_MEMBER_INVALID);
		}
	}

}
