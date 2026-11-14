package tr.okul.iletisim.dms;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.directory.AudienceResolver;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.TargetType;
import tr.okul.iletisim.dms.DmsDtos.GrantCreateRequest;
import tr.okul.iletisim.dms.DmsDtos.GrantResult;
import tr.okul.iletisim.dms.DmsDtos.GrantRow;
import tr.okul.iletisim.dms.DmsDtos.GrantUpdateRequest;
import tr.okul.iletisim.dms.DmsDtos.SpaceRequest;
import tr.okul.iletisim.dms.DmsDtos.SpaceRow;

@Service
public class DmsSpaceService {

	private static final int GRANT_LIMIT = 100;

	private final DmsSpaceRepository spaces;
	private final DmsSpaceGrantRepository grants;
	private final DmsFolderRepository folders;
	private final DmsFileRepository files;
	private final DmsAccess access;
	private final DmsQueries queries;
	private final AudienceResolver audience;
	private final AuditService audit;
	private final NotificationService notifications;
	private final JdbcClient jdbc;

	public DmsSpaceService(DmsSpaceRepository spaces, DmsSpaceGrantRepository grants, DmsFolderRepository folders,
			DmsFileRepository files, DmsAccess access, DmsQueries queries, AudienceResolver audience,
			AuditService audit, NotificationService notifications, JdbcClient jdbc) {
		this.spaces = spaces;
		this.grants = grants;
		this.folders = folders;
		this.files = files;
		this.access = access;
		this.queries = queries;
		this.audience = audience;
		this.audit = audit;
		this.notifications = notifications;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<SpaceRow> list(UserContext me) {
		me.requireMember();
		return queries.spaces(me);
	}

	@Transactional(readOnly = true)
	public SpaceRow get(UserContext me, long id) {
		me.requireMember();
		DmsLevel level = access.require(me, id, DmsLevel.VIEW);
		return queries.space(id, level);
	}

	@Transactional
	public SpaceRow create(UserContext me, SpaceRequest req) {
		me.requireMember();
		if (!me.canCreateDmsSpace()) {
			throw new BusinessException(DmsErrorCode.DMS_CREATE_FORBIDDEN);
		}
		String name = spaceName(req.name());
		String key = TextNormalizer.fold(name);
		if (spaces.existsByNameKey(key)) {
			throw BusinessException.field(DmsErrorCode.DMS_SPACE_NAME_TAKEN, "name");
		}
		DmsSpace s = new DmsSpace();
		s.setName(name);
		s.setNameKey(key);
		s.setDescription(description(req.description()));
		s.setOwnerId(me.userId());
		s = spaces.saveAndFlush(s);
		audit.log(me.userId(), DmsAuditAction.DMS_SPACE_CREATED, "DMS_SPACE", s.getId(), detail(s, "SPACE"));
		return queries.space(s.getId(), DmsLevel.MANAGE);
	}

	@Transactional
	public SpaceRow update(UserContext me, long id, SpaceRequest req) {
		me.requireMember();
		DmsLevel level = access.require(me, id, DmsLevel.MANAGE);
		DmsSpace s = load(id);
		String name = spaceName(req.name());
		String key = TextNormalizer.fold(name);
		if (spaces.existsByNameKeyAndIdNot(key, id)) {
			throw BusinessException.field(DmsErrorCode.DMS_SPACE_NAME_TAKEN, "name");
		}
		s.setName(name);
		s.setNameKey(key);
		s.setDescription(description(req.description()));
		spaces.saveAndFlush(s);
		audit.log(me.userId(), DmsAuditAction.DMS_SPACE_UPDATED, "DMS_SPACE", id, detail(s, "SPACE"));
		return queries.space(id, level);
	}

	@Transactional
	public void delete(UserContext me, long id) {
		me.requireMember();
		access.require(me, id, DmsLevel.MANAGE);
		DmsSpace s = load(id);
		if (folders.existsBySpaceId(id) || files.existsBySpaceId(id)) {
			throw new BusinessException(DmsErrorCode.DMS_SPACE_NOT_EMPTY);
		}
		spaces.delete(s);
		spaces.flush();
		audit.log(me.userId(), DmsAuditAction.DMS_SPACE_DELETED, "DMS_SPACE", id, detail(s, "SPACE"));
	}

	@Transactional(readOnly = true)
	public List<GrantRow> grants(UserContext me, long spaceId) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.MANAGE);
		return queries.grants(spaceId);
	}

	@Transactional
	public GrantResult addGrant(UserContext me, long spaceId, GrantCreateRequest req) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.MANAGE);
		DmsSpace space = load(spaceId);
		TargetRef subject = req.subject();
		audience.validate(me, List.of(subject), EnumSet.allOf(TargetType.class));
		if (subject.type() == TargetType.ALL) {
			subject = new TargetRef(TargetType.ALL, null);
		}
		if (subject.type() == TargetType.USER && subject.id().equals(space.getOwnerId())) {
			throw new BusinessException(DmsErrorCode.DMS_GRANT_OWNER);
		}
		DmsSpaceGrant existing = subject.type() == TargetType.ALL
				? grants.findBySpaceIdAndSubjectTypeAndSubjectIdIsNull(spaceId, TargetType.ALL).orElse(null)
				: grants.findBySpaceIdAndSubjectTypeAndSubjectId(spaceId, subject.type(), subject.id()).orElse(null);
		boolean created = existing == null;
		DmsSpaceGrant g = existing;
		if (created) {
			if (grants.countBySpaceId(spaceId) >= GRANT_LIMIT) {
				throw new BusinessException(DmsErrorCode.DMS_GRANT_LIMIT);
			}
			g = new DmsSpaceGrant();
			g.setSpaceId(spaceId);
			g.setSubjectType(subject.type());
			g.setSubjectId(subject.id());
			g.setCreatedBy(me.userId());
		}
		g.setLevel(req.level());
		g = grants.saveAndFlush(g);
		GrantRow row = queries.grant(g.getId());
		auditGrant(me, space, row, "ADD");
		if (created && subject.type() == TargetType.USER) {
			notifications.notify(List.of(subject.id()), "DMS_SPACE_SHARED",
					me.fullName() + " sizinle bir doküman alanı paylaştı: " + space.getName(), null,
					"/dokumanlar/" + spaceId);
		}
		return new GrantResult(row, created);
	}

	@Transactional
	public GrantRow updateGrant(UserContext me, long spaceId, long grantId, GrantUpdateRequest req) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.MANAGE);
		DmsSpace space = load(spaceId);
		DmsSpaceGrant g = loadGrant(spaceId, grantId);
		g.setLevel(req.level());
		grants.saveAndFlush(g);
		GrantRow row = queries.grant(grantId);
		auditGrant(me, space, row, "UPDATE");
		return row;
	}

	@Transactional
	public void removeGrant(UserContext me, long spaceId, long grantId) {
		me.requireMember();
		access.require(me, spaceId, DmsLevel.MANAGE);
		DmsSpace space = load(spaceId);
		DmsSpaceGrant g = loadGrant(spaceId, grantId);
		GrantRow row = queries.grant(grantId);
		grants.delete(g);
		grants.flush();
		auditGrant(me, space, row, "REMOVE");
	}

	@Transactional(readOnly = true)
	public int ownedCount(long userId) {
		return spaces.findByOwnerId(userId).size();
	}

	@Transactional
	public void transferOwnership(long userId, Long successorUserId) {
		if (successorUserId != null) {
			for (DmsSpace s : spaces.findByOwnerId(userId)) {
				s.setOwnerId(successorUserId);
				spaces.saveAndFlush(s);
				jdbc.sql("DELETE FROM dms_space_grant WHERE space_id = :sid AND subject_type = 'USER' AND subject_id = :uid")
						.param("sid", s.getId()).param("uid", successorUserId).update();
				notifications.notify(List.of(successorUserId), "DMS_SPACE_OWNER_CHANGED",
						"Doküman alanının yeni sahibi oldunuz: " + s.getName(), null, "/dokumanlar/" + s.getId());
			}
		}
		jdbc.sql("DELETE FROM dms_space_grant WHERE subject_type = 'USER' AND subject_id = :uid")
				.param("uid", userId).update();
	}

	private void auditGrant(UserContext me, DmsSpace space, GrantRow row, String op) {
		Map<String, Object> d = detail(space, "SPACE");
		d.put("subject", row.subject());
		d.put("level", row.level().name());
		d.put("op", op);
		audit.log(me.userId(), DmsAuditAction.DMS_GRANT_CHANGED, "DMS_SPACE", space.getId(), d);
	}

	private static Map<String, Object> detail(DmsSpace s, String type) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("spaceId", s.getId());
		d.put("name", s.getName());
		d.put("type", type);
		return d;
	}

	private DmsSpace load(long id) {
		return spaces.findById(id).orElseThrow(() -> new BusinessException(DmsErrorCode.DMS_SPACE_NOT_FOUND));
	}

	private DmsSpaceGrant loadGrant(long spaceId, long grantId) {
		return grants.findByIdAndSpaceId(grantId, spaceId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
	}

	private static String spaceName(String raw) {
		String name = TextNormalizer.clean(raw);
		if (name == null || name.length() < 2 || name.length() > 100) {
			throw BusinessException.field(CommonErrorCode.VALIDATION_FAILED, "name");
		}
		return name;
	}

	private static String description(String raw) {
		String d = raw == null ? null : raw.trim();
		return d == null || d.isEmpty() ? null : d;
	}

}
