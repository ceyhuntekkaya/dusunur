package tr.okul.iletisim.print;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.print.PrintDtos.MonthlyReport;
import tr.okul.iletisim.print.PrintDtos.PersonRef;
import tr.okul.iletisim.print.PrintDtos.PrintActions;
import tr.okul.iletisim.print.PrintDtos.PrintFileInfo;
import tr.okul.iletisim.print.PrintDtos.PrintHistoryItem;
import tr.okul.iletisim.print.PrintDtos.PrintRequestDetail;
import tr.okul.iletisim.print.PrintDtos.PrintRequestRow;
import tr.okul.iletisim.print.PrintDtos.PrintSeason;
import tr.okul.iletisim.print.PrintDtos.QueueBoard;
import tr.okul.iletisim.print.PrintDtos.ReportRow;
import tr.okul.iletisim.print.PrintDtos.ReportTotals;

@Component
public class PrintQueries {

	public static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
	public static final String SECRET_TITLE = "Gizli evrak";

	private record RowDb(long id, long seasonId, String title, boolean secret, String status, long requesterId,
			String requesterName, String branchName, int pageCount, int copies, int totalPages, String color,
			String sides, String paperSize, boolean originalOnPaper, OffsetDateTime dueAt, OffsetDateTime createdAt,
			Long approverId, String approverName, String approverBranch) {
	}

	private record PersonDb(long id, String fullName, String branchName) {
	}

	private static final String ROW_SELECT = """
			SELECT r.id, r.season_id, r.title, r.secret, r.status, r.requester_id,
			       ru.first_name || ' ' || ru.last_name AS requester_name, b.name AS branch_name,
			       r.page_count, r.copies, r.total_pages, r.color, r.sides, r.paper_size, r.original_on_paper,
			       r.due_at, r.created_at, r.approver_id, au.first_name || ' ' || au.last_name AS approver_name,
			       ab.name AS approver_branch
			FROM print_request r
			JOIN app_user ru ON ru.id = r.requester_id
			JOIN branch b ON b.id = r.branch_id
			LEFT JOIN app_user au ON au.id = r.approver_id
			LEFT JOIN season_membership am ON am.season_id = r.season_id AND am.user_id = r.approver_id
			LEFT JOIN branch ab ON ab.id = am.branch_id
			""";

	private final JdbcClient jdbc;
	private final StoredFileRepository storedFiles;
	private final PrintRequestHistoryRepository histories;

	public PrintQueries(JdbcClient jdbc, StoredFileRepository storedFiles, PrintRequestHistoryRepository histories) {
		this.jdbc = jdbc;
		this.storedFiles = storedFiles;
		this.histories = histories;
	}

	public List<PrintSeason> seasons(long uid) {
		return jdbc.sql("""
				SELECT s.id, s.name, (s.status = 'ACTIVE') AS active
				FROM season s JOIN season_membership m ON m.season_id = s.id AND m.user_id = :uid
				WHERE s.status <> 'DRAFT'
				ORDER BY s.name DESC
				""").param("uid", uid).query(PrintSeason.class).list();
	}

	public PageResponse<PrintRequestRow> mine(UserContext me, Long seasonId, String group, int page, int size) {
		long season = seasonId == null ? me.requireMember() : seasonId;
		String cond = switch (group == null || group.isBlank() ? "all" : group) {
			case "all" -> "";
			case "open" -> " AND r.status IN ('PENDING_APPROVAL','APPROVED','IN_PROGRESS')";
			case "ready" -> " AND r.status = 'READY'";
			case "delivered" -> " AND r.status = 'DELIVERED'";
			case "closed" -> " AND r.status IN ('REJECTED','CANCELLED')";
			default -> throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, "group");
		};
		Map<String, Object> params = new HashMap<>();
		params.put("uid", me.userId());
		params.put("sid", season);
		return paged(me, "r.requester_id = :uid AND r.season_id = :sid" + cond, "r.id DESC", params, page, size);
	}

	public PageResponse<PrintRequestRow> approvals(UserContext me, String tab, int page, int size) {
		long season = me.requireMember();
		Map<String, Object> params = new HashMap<>();
		params.put("uid", me.userId());
		params.put("sid", season);
		return switch (tab == null || tab.isBlank() ? "pending" : tab) {
			case "pending" -> paged(me, "r.approver_id = :uid AND r.status = 'PENDING_APPROVAL' AND r.season_id = :sid",
					"r.due_at ASC, r.id ASC", params, page, size);
			case "decided" -> paged(me, "r.decided_by = :uid AND r.season_id = :sid", "r.decided_at DESC, r.id DESC",
					params, page, size);
			default -> throw new BusinessException(CommonErrorCode.REQUEST_INVALID_PARAM, "tab");
		};
	}

	public QueueBoard board(UserContext me) {
		return new QueueBoard(queue(me, "APPROVED"), queue(me, "IN_PROGRESS"), queue(me, "READY"));
	}

	private List<PrintRequestRow> queue(UserContext me, String status) {
		return jdbc.sql(ROW_SELECT + " WHERE r.season_id = :sid AND r.branch_id = :bid AND r.status = :st"
				+ " ORDER BY r.due_at ASC, r.id ASC")
				.param("sid", me.seasonId()).param("bid", me.membership().branchId()).param("st", status)
				.query(RowDb.class).list().stream().map(r -> toRow(me, r)).toList();
	}

	public PageResponse<PrintRequestRow> delivered(UserContext me, LocalDate date, int page, int size) {
		Map<String, Object> params = new HashMap<>();
		params.put("sid", me.seasonId());
		params.put("bid", me.membership().branchId());
		String cond = "";
		if (date != null) {
			cond = " AND r.delivered_at >= :from AND r.delivered_at < :to";
			params.put("from", date.atStartOfDay(ISTANBUL).toOffsetDateTime());
			params.put("to", date.plusDays(1).atStartOfDay(ISTANBUL).toOffsetDateTime());
		}
		return paged(me, "r.season_id = :sid AND r.branch_id = :bid AND r.status = 'DELIVERED'" + cond,
				"r.delivered_at DESC, r.id DESC", params, page, size);
	}

	private PageResponse<PrintRequestRow> paged(UserContext me, String where, String order, Map<String, Object> params,
			int page, int size) {
		int p = Math.max(page, 0);
		int s = PageResponse.clampSize(size);
		long total = jdbc.sql("SELECT count(*) FROM print_request r WHERE " + where).params(params)
				.query(Long.class).single();
		List<RowDb> rows = jdbc.sql(ROW_SELECT + " WHERE " + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
				.params(params).param("limit", s).param("offset", (long) p * s).query(RowDb.class).list();
		return new PageResponse<>(rows.stream().map(r -> toRow(me, r)).toList(), p, s, total);
	}

	private PrintRequestRow toRow(UserContext me, RowDb r) {
		boolean masked = r.secret() && r.requesterId() != me.userId();
		boolean archived = me.seasonId() == null || r.seasonId() != me.seasonId();
		PersonRef approver = r.approverId() == null ? null
				: new PersonRef(r.approverId(), r.approverName(), r.approverBranch());
		return new PrintRequestRow(r.id(), r.seasonId(), archived, masked ? SECRET_TITLE : r.title(), r.secret(),
				PrintStatus.valueOf(r.status()), new PersonRef(r.requesterId(), r.requesterName(), r.branchName()),
				r.branchName(), r.pageCount(), r.copies(), r.totalPages(), PrintColor.valueOf(r.color()),
				PrintSides.valueOf(r.sides()), PaperSize.valueOf(r.paperSize()), r.originalOnPaper(), r.dueAt(),
				r.createdAt(), approver);
	}

	public PrintRequestDetail detail(UserContext me, PrintRequest r, PrintActions actions) {
		RowDb db = jdbc.sql(ROW_SELECT + " WHERE r.id = :id").param("id", r.getId()).query(RowDb.class).single();
		PrintRequestRow row = toRow(me, db);
		List<PrintRequestHistory> history = histories.findByRequestIdOrderByIdAsc(r.getId());
		Set<Long> ids = new HashSet<>();
		for (Long id : new Long[] { r.getOriginalApproverId(), r.getDecidedBy(), r.getOperatorId() }) {
			if (id != null) {
				ids.add(id);
			}
		}
		history.forEach(h -> {
			if (h.getActorId() != null) {
				ids.add(h.getActorId());
			}
		});
		Map<Long, PersonRef> persons = persons(ids, r.getSeasonId());
		List<PrintHistoryItem> items = new ArrayList<>();
		for (PrintRequestHistory h : history) {
			items.add(new PrintHistoryItem(h.getFromStatus(), h.getToStatus(),
					h.getActorId() == null ? null : persons.get(h.getActorId()), h.getNote(), h.getCreatedAt()));
		}
		PrintFileInfo file = null;
		if (r.getFileId() != null) {
			StoredFile f = storedFiles.findById(r.getFileId()).orElse(null);
			if (f != null) {
				file = new PrintFileInfo(f.getOriginalName(), f.getContentType(), f.getSizeBytes(),
						r.getFilePurgedAt() != null);
			}
		}
		return new PrintRequestDetail(row.id(), row.seasonId(), row.archived(), r.getTitle(), row.secret(),
				row.status(), row.requester(), row.branchName(), row.pageCount(), row.copies(), row.totalPages(),
				row.color(), row.sides(), row.paperSize(), row.originalOnPaper(), row.dueAt(), row.createdAt(),
				row.approver(), r.getNote(), r.getRejectReason(), r.getCancelReason(),
				persons.get(r.getOriginalApproverId()), persons.get(r.getDecidedBy()), r.getDecidedAt(),
				persons.get(r.getOperatorId()), r.getStartedAt(), r.getReadyAt(), r.getDeliveredAt(),
				r.getCancelledAt(), file, items, actions);
	}

	public Map<Long, PersonRef> persons(Collection<Long> ids, long seasonId) {
		Map<Long, PersonRef> result = new HashMap<>();
		if (ids.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				SELECT u.id, u.first_name || ' ' || u.last_name AS full_name, b.name AS branch_name
				FROM app_user u
				LEFT JOIN season_membership m ON m.user_id = u.id AND m.season_id = :sid
				LEFT JOIN branch b ON b.id = m.branch_id
				WHERE u.id IN (:ids)
				""").param("sid", seasonId).param("ids", ids).query(PersonDb.class).list()
				.forEach(p -> result.put(p.id(), new PersonRef(p.id(), p.fullName(), p.branchName())));
		return result;
	}

	public PersonRef person(long userId, long seasonId) {
		return persons(List.of(userId), seasonId).get(userId);
	}

	public String fullName(long userId) {
		return jdbc.sql("SELECT first_name || ' ' || last_name FROM app_user WHERE id = :id").param("id", userId)
				.query(String.class).single();
	}

	public String branchName(long branchId) {
		return jdbc.sql("SELECT name FROM branch WHERE id = :id").param("id", branchId).query(String.class).single();
	}

	public List<Long> operatorIds(long seasonId, long branchId) {
		return jdbc.sql("""
				SELECT m.user_id FROM season_membership m JOIN app_user u ON u.id = m.user_id
				WHERE m.season_id = :sid AND m.branch_id = :bid AND m.is_print_operator AND u.status = 'ACTIVE'
				""").param("sid", seasonId).param("bid", branchId).query(Long.class).list();
	}

	public boolean isApprover(long seasonId, long uid) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM season_membership WHERE season_id = :sid AND print_approver_id = :uid)
				    OR EXISTS (SELECT 1 FROM print_request WHERE season_id = :sid AND approver_id = :uid
				               AND status IN ('PENDING_APPROVAL','APPROVED','IN_PROGRESS','READY'))
				""").param("sid", seasonId).param("uid", uid).query(Boolean.class).single();
	}

	public boolean isActiveSeasonMember(long seasonId, long uid) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM season_membership m JOIN app_user u ON u.id = m.user_id
				               WHERE m.season_id = :sid AND m.user_id = :uid AND u.status = 'ACTIVE')
				""").param("sid", seasonId).param("uid", uid).query(Boolean.class).single();
	}

	public int pendingApprovalCount(long seasonId, long uid) {
		return jdbc.sql("""
				SELECT count(*) FROM print_request
				WHERE season_id = :sid AND approver_id = :uid AND status = 'PENDING_APPROVAL'
				""").param("sid", seasonId).param("uid", uid).query(Integer.class).single();
	}

	public int pendingApprovalCount(long uid) {
		return jdbc.sql("SELECT count(*) FROM print_request WHERE approver_id = :uid AND status = 'PENDING_APPROVAL'")
				.param("uid", uid).query(Integer.class).single();
	}

	public int queueCount(long seasonId, long branchId) {
		return jdbc.sql("""
				SELECT count(*) FROM print_request
				WHERE season_id = :sid AND branch_id = :bid AND status IN ('APPROVED','IN_PROGRESS')
				""").param("sid", seasonId).param("bid", branchId).query(Integer.class).single();
	}

	public int openCount(long seasonId) {
		return jdbc.sql("""
				SELECT count(*) FROM print_request
				WHERE season_id = :sid AND status IN ('PENDING_APPROVAL','APPROVED','IN_PROGRESS','READY')
				""").param("sid", seasonId).query(Integer.class).single();
	}

	public List<Long> purgeCandidates(OffsetDateTime cutoff, int limit) {
		return jdbc.sql("""
				SELECT id FROM print_request
				WHERE file_id IS NOT NULL AND file_purged_at IS NULL AND (
				      (status = 'DELIVERED' AND delivered_at < :cutoff)
				   OR (status = 'REJECTED' AND decided_at < :cutoff)
				   OR (status = 'CANCELLED' AND cancelled_at < :cutoff))
				ORDER BY id LIMIT :limit
				""").param("cutoff", cutoff).param("limit", limit).query(Long.class).list();
	}

	public MonthlyReport report(String month, LocalDate first, Long branchId) {
		OffsetDateTime from = first.atStartOfDay(ISTANBUL).toOffsetDateTime();
		OffsetDateTime to = first.plusMonths(1).atStartOfDay(ISTANBUL).toOffsetDateTime();
		List<ReportRow> rows = jdbc.sql("""
				SELECT r.requester_id AS user_id, u.first_name || ' ' || u.last_name AS full_name,
				       string_agg(DISTINCT b.name, ', ') AS branch_name, count(*) AS request_count,
				       COALESCE(sum(CASE WHEN r.color = 'BW' THEN r.total_pages END), 0) AS bw_pages,
				       COALESCE(sum(CASE WHEN r.color = 'COLOR' THEN r.total_pages END), 0) AS color_pages,
				       sum(r.total_pages) AS total_pages
				FROM print_request r
				JOIN app_user u ON u.id = r.requester_id
				JOIN branch b ON b.id = r.branch_id
				WHERE r.status = 'DELIVERED' AND r.delivered_at >= :from AND r.delivered_at < :to
				  AND (CAST(:branchId AS bigint) IS NULL OR r.branch_id = CAST(:branchId AS bigint))
				GROUP BY r.requester_id, u.first_name, u.last_name
				ORDER BY total_pages DESC, full_name
				""").param("from", from).param("to", to).param("branchId", branchId, java.sql.Types.BIGINT)
				.query(ReportRow.class).list();
		long count = 0;
		long bw = 0;
		long color = 0;
		long total = 0;
		for (ReportRow r : rows) {
			count += r.requestCount();
			bw += r.bwPages();
			color += r.colorPages();
			total += r.totalPages();
		}
		return new MonthlyReport(month, rows, new ReportTotals(count, bw, color, total));
	}

}
