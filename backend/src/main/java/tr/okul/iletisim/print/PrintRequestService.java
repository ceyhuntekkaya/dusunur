package tr.okul.iletisim.print;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.audit.AuditService;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.file.FilePurpose;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.file.StoredFile;
import tr.okul.iletisim.common.file.StoredFileRepository;
import tr.okul.iletisim.common.notify.NotificationService;
import tr.okul.iletisim.common.notify.RealtimePublisher;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.print.PrintDtos.CreatePrintRequest;
import tr.okul.iletisim.print.PrintDtos.OnLeave;
import tr.okul.iletisim.print.PrintDtos.PersonRef;
import tr.okul.iletisim.print.PrintDtos.PrintContext;
import tr.okul.iletisim.print.PrintDtos.PrintRequestDetail;

@Service
public class PrintRequestService {

	private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("d MMM EEE HH:mm",
			Locale.forLanguageTag("tr"));

	private final PrintRequestRepository requests;
	private final PrintRequestHistoryRepository histories;
	private final PrintDelegationRepository delegations;
	private final PrintAccess access;
	private final PrintQueries queries;
	private final PrintPageCounter pageCounter;
	private final FileStorageService files;
	private final StoredFileRepository storedFiles;
	private final NotificationService notifications;
	private final RealtimePublisher realtime;
	private final AuditService audit;
	private final AppProperties app;

	public PrintRequestService(PrintRequestRepository requests, PrintRequestHistoryRepository histories,
			PrintDelegationRepository delegations, PrintAccess access, PrintQueries queries,
			PrintPageCounter pageCounter, FileStorageService files, StoredFileRepository storedFiles,
			NotificationService notifications, RealtimePublisher realtime, AuditService audit, AppProperties app) {
		this.requests = requests;
		this.histories = histories;
		this.delegations = delegations;
		this.access = access;
		this.queries = queries;
		this.pageCounter = pageCounter;
		this.files = files;
		this.storedFiles = storedFiles;
		this.notifications = notifications;
		this.realtime = realtime;
		this.audit = audit;
		this.app = app;
	}

	@Transactional(readOnly = true)
	public PrintContext context(UserContext me) {
		long season = me.requireMember();
		UserContext.Membership m = me.membership();
		PersonRef myApprover = null;
		OnLeave onLeave = null;
		if (m.printApproverId() != null) {
			long a = m.printApproverId();
			PrintDelegation d = activeDelegation(a, season);
			if (d != null && d.getDelegateId() != me.userId()) {
				Map<Long, PersonRef> refs = queries.persons(List.of(a, d.getDelegateId()), season);
				myApprover = refs.get(d.getDelegateId());
				onLeave = new OnLeave(refs.get(a), myApprover, d.getEndDate());
			}
			else {
				myApprover = queries.person(a, season);
			}
		}
		return new PrintContext(queries.isApprover(season, me.userId()), m.printOperator(), m.manager(),
				m.branchName(), myApprover, onLeave, app.print().minLeadMinutes(), app.print().maxDaysAhead());
	}

	@Transactional
	public PrintRequestDetail create(UserContext me, CreatePrintRequest req) {
		long seasonId = me.requireMember();
		String title = req.title().trim();
		String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
		if ((long) req.pageCount() * req.copies() > 20000) {
			throw BusinessException.field(PrintErrorCode.PRINT_TOTAL_TOO_LARGE, "copies");
		}
		if (!req.originalOnPaper() && req.fileId() == null) {
			throw BusinessException.field(PrintErrorCode.PRINT_FILE_REQUIRED, "fileId");
		}
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		if (req.dueAt().isBefore(now.plusMinutes(app.print().minLeadMinutes()))) {
			throw BusinessException.field(PrintErrorCode.PRINT_DUE_TOO_SOON, "dueAt", app.print().minLeadMinutes());
		}
		if (req.dueAt().isAfter(now.plusDays(app.print().maxDaysAhead()))) {
			throw BusinessException.field(PrintErrorCode.PRINT_DUE_TOO_LATE, "dueAt", app.print().maxDaysAhead());
		}
		if (req.copiedFromId() != null && requests.findById(req.copiedFromId())
				.filter(c -> c.getRequesterId() == me.userId()).isEmpty()) {
			throw new BusinessException(PrintErrorCode.PRINT_REQUEST_NOT_FOUND);
		}

		UUID fileId = null;
		if (!req.originalOnPaper()) {
			fileId = files.attach(req.fileId(), me, FilePurpose.PRINT).getId();
		}

		Long approverId = null;
		Long originalApproverId = null;
		String note0;
		Long a = me.membership().printApproverId();
		if (a == null) {
			note0 = "Onaylayıcı tanımlı değil; otomatik onaylandı.";
		}
		else {
			PrintDelegation d = activeDelegation(a, seasonId);
			if (d != null && d.getDelegateId() != me.userId()) {
				approverId = d.getDelegateId();
				originalApproverId = a;
				note0 = "%s izinde; talep vekil %s kişisine yönlendirildi.".formatted(queries.fullName(a),
						queries.fullName(approverId));
			}
			else {
				approverId = a;
				note0 = null;
			}
		}

		PrintRequest r = new PrintRequest();
		r.setSeasonId(seasonId);
		r.setRequesterId(me.userId());
		r.setBranchId(me.membership().branchId());
		r.setTitle(title);
		r.setFileId(fileId);
		r.setOriginalOnPaper(req.originalOnPaper());
		r.setPageCount(req.pageCount());
		r.setCopies(req.copies());
		r.setColor(req.color());
		r.setSides(req.sides());
		r.setPaperSize(req.paperSize());
		r.setSecret(req.secret());
		r.setDueAt(req.dueAt());
		r.setNote(note);
		r.setTotalPages(req.pageCount() * req.copies());
		r.setStatus(approverId == null ? PrintStatus.APPROVED : PrintStatus.PENDING_APPROVAL);
		r.setApproverId(approverId);
		r.setOriginalApproverId(originalApproverId);
		r.setCopiedFromId(req.copiedFromId());
		r = requests.saveAndFlush(r);
		onChanged(r, null, me.userId(), note0);
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail cancel(UserContext me, long id) {
		me.requireMember();
		PrintRequest r = loadVisible(me, id);
		if (!access.isOwner(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_CANCEL_NOT_ALLOWED);
		}
		me.requireWritable(r.getSeasonId());
		if (r.getStatus() != PrintStatus.PENDING_APPROVAL && r.getStatus() != PrintStatus.APPROVED) {
			throw new BusinessException(PrintErrorCode.PRINT_CANCEL_NOT_ALLOWED);
		}
		r.setCancelledBy(me.userId());
		r.setCancelledAt(now());
		r.setCancelReason("Talep sahibi iptal etti");
		transition(r, PrintStatus.CANCELLED, me.userId(), "Talep sahibi iptal etti");
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail approve(UserContext me, long id) {
		PrintRequest r = loadForDecision(me, id);
		r.setDecidedBy(me.userId());
		r.setDecidedAt(now());
		transition(r, PrintStatus.APPROVED, me.userId(), null);
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail reject(UserContext me, long id, String reason) {
		PrintRequest r = loadForDecision(me, id);
		String trimmed = reason.trim();
		r.setDecidedBy(me.userId());
		r.setDecidedAt(now());
		r.setRejectReason(trimmed);
		transition(r, PrintStatus.REJECTED, me.userId(), trimmed);
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail start(UserContext me, long id) {
		PrintRequest r = loadForOperator(me, id);
		requireStatus(r, PrintStatus.APPROVED);
		r.setOperatorId(me.userId());
		r.setStartedAt(now());
		transition(r, PrintStatus.IN_PROGRESS, me.userId(), null);
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail ready(UserContext me, long id) {
		PrintRequest r = loadForOperator(me, id);
		requireStatus(r, PrintStatus.APPROVED, PrintStatus.IN_PROGRESS);
		if (r.getOperatorId() == null) {
			r.setOperatorId(me.userId());
		}
		r.setReadyAt(now());
		transition(r, PrintStatus.READY, me.userId(), null);
		return detailOf(me, r);
	}

	@Transactional
	public PrintRequestDetail deliver(UserContext me, long id) {
		PrintRequest r = loadForOperator(me, id);
		requireStatus(r, PrintStatus.READY);
		r.setDeliveredAt(now());
		transition(r, PrintStatus.DELIVERED, me.userId(), null);
		return detailOf(me, r);
	}

	@Transactional(readOnly = true)
	public PrintRequestDetail detail(UserContext me, long id) {
		me.requireMember();
		PrintRequest r = load(id);
		if (!access.canView(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_REQUEST_NOT_FOUND);
		}
		return detailOf(me, r);
	}

	@Transactional
	public StoredFile file(UserContext me, long id) {
		me.requireMember();
		PrintRequest r = load(id);
		if (!access.canAccessFile(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_REQUEST_NOT_FOUND);
		}
		if (r.getFileId() == null) {
			throw new BusinessException(PrintErrorCode.PRINT_NO_FILE);
		}
		if (r.getFilePurgedAt() != null) {
			throw new BusinessException(PrintErrorCode.PRINT_FILE_PURGED);
		}
		StoredFile f = files.get(r.getFileId());
		if (r.isSecret() && !access.isOwner(me, r)) {
			audit.log(me.userId(), PrintAuditAction.PRINT_SECRET_FILE_ACCESSED, "PRINT_REQUEST", id,
					Map.of("requestId", id));
		}
		return f;
	}

	@Transactional(readOnly = true)
	public Integer pageCount(UserContext me, UUID fileId) {
		me.requireMember();
		StoredFile f = storedFiles.findById(fileId).orElse(null);
		if (f == null || f.getDeletedAt() != null || f.isAttached() || f.getPurpose() != FilePurpose.PRINT
				|| !f.getUploadedBy().equals(me.userId())) {
			throw new BusinessException(tr.okul.iletisim.common.error.CommonErrorCode.FILE_NOT_USABLE);
		}
		if (!"application/pdf".equals(f.getContentType())) {
			return null;
		}
		try {
			Resource res = files.serve(f, false).getBody();
			return res == null ? null : pageCounter.count(res);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	@Transactional
	public int cancelOpenForSeason(long closedSeasonId) {
		List<PrintRequest> open = requests.findBySeasonIdAndStatusIn(closedSeasonId, PrintStatus.OPEN);
		for (PrintRequest r : open) {
			r.setCancelledAt(now());
			r.setCancelReason("Eğitim-öğretim yılı kapandı");
			transition(r, PrintStatus.CANCELLED, null, "Eğitim-öğretim yılı kapandı");
		}
		return open.size();
	}

	@Transactional
	public void transferApprover(long fromUserId, Long toUserId) {
		if (toUserId == null) {
			return;
		}
		for (PrintRequest r : requests.findByApproverIdAndStatus(fromUserId, PrintStatus.PENDING_APPROVAL)) {
			if (r.getRequesterId().equals(toUserId)) {
				r.setApproverId(null);
				transition(r, PrintStatus.APPROVED, null, "Onaylayıcı ayrıldı; otomatik onaylandı.");
			}
			else {
				r.setApproverId(toUserId);
				requests.saveAndFlush(r);
				onChanged(r, PrintStatus.PENDING_APPROVAL, null, "Onaylayıcı değişti: " + queries.fullName(toUserId) + ".");
			}
		}
	}

	@Transactional
	public void purgeFile(long id) {
		PrintRequest r = requests.findById(id).orElse(null);
		if (r == null || r.getFileId() == null || r.getFilePurgedAt() != null) {
			return;
		}
		StoredFile f = storedFiles.findById(r.getFileId()).orElse(null);
		if (f != null && f.getDeletedAt() == null) {
			files.delete(f);
		}
		r.setFilePurgedAt(now());
		requests.save(r);
	}

	private PrintRequestDetail detailOf(UserContext me, PrintRequest r) {
		return queries.detail(me, r, access.actions(me, r));
	}

	private PrintRequest load(long id) {
		return requests.findById(id)
				.orElseThrow(() -> new BusinessException(PrintErrorCode.PRINT_REQUEST_NOT_FOUND));
	}

	private PrintRequest loadVisible(UserContext me, long id) {
		PrintRequest r = load(id);
		if (!access.canView(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_REQUEST_NOT_FOUND);
		}
		return r;
	}

	private PrintRequest loadForDecision(UserContext me, long id) {
		me.requireMember();
		PrintRequest r = load(id);
		if (!access.isAssignedApprover(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_APPROVE_FORBIDDEN);
		}
		me.requireWritable(r.getSeasonId());
		requireStatus(r, PrintStatus.PENDING_APPROVAL);
		return r;
	}

	private PrintRequest loadForOperator(UserContext me, long id) {
		me.requireMember();
		PrintRequest r = loadVisible(me, id);
		if (!access.isOperator(me, r)) {
			throw new BusinessException(PrintErrorCode.PRINT_OPERATOR_ONLY);
		}
		return r;
	}

	private void requireStatus(PrintRequest r, PrintStatus... allowed) {
		for (PrintStatus s : allowed) {
			if (r.getStatus() == s) {
				return;
			}
		}
		throw new BusinessException(PrintErrorCode.PRINT_INVALID_STATUS, r.getStatus().label());
	}

	private PrintDelegation activeDelegation(long approverId, long seasonId) {
		LocalDate today = LocalDate.now(PrintQueries.ISTANBUL);
		return delegations.findFirstByApproverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(approverId, today,
				today).filter(d -> queries.isActiveSeasonMember(seasonId, d.getDelegateId())).orElse(null);
	}

	private static OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private void transition(PrintRequest r, PrintStatus to, Long actorId, String note) {
		PrintStatus from = r.getStatus();
		r.setStatus(to);
		requests.saveAndFlush(r);
		onChanged(r, from, actorId, note);
	}

	private void onChanged(PrintRequest r, PrintStatus from, Long actorId, String note) {
		PrintStatus to = r.getStatus();
		PrintRequestHistory h = new PrintRequestHistory();
		h.setRequestId(r.getId());
		h.setFromStatus(from);
		h.setToStatus(to);
		h.setActorId(actorId);
		h.setNote(note);
		histories.save(h);

		String link = "/fotokopi/" + r.getId();
		String masked = r.isSecret() ? PrintQueries.SECRET_TITLE : r.getTitle();
		List<Long> operators = queries.operatorIds(r.getSeasonId(), r.getBranchId());
		switch (to) {
			case PENDING_APPROVAL -> notifications.notify(List.of(r.getApproverId()), "PRINT_APPROVAL_REQUESTED",
					"Onay bekleyen fotokopi talebi", "%s: %s – %d sayfa × %d kopya".formatted(
							queries.fullName(r.getRequesterId()), masked, r.getPageCount(), r.getCopies()),
					link);
			case APPROVED -> {
				if (from == PrintStatus.PENDING_APPROVAL && actorId != null) {
					notifications.notify(List.of(r.getRequesterId()), "PRINT_APPROVED",
							"Fotokopi talebiniz onaylandı", r.getTitle(), link);
				}
				notifications.notify(operators, "PRINT_QUEUE_NEW", "Yeni fotokopi talebi",
						"%s · Teslim: %s".formatted(queries.fullName(r.getRequesterId()),
								r.getDueAt().atZoneSameInstant(PrintQueries.ISTANBUL).format(DUE_FORMAT)),
						link);
			}
			case REJECTED -> notifications.notify(List.of(r.getRequesterId()), "PRINT_REJECTED",
					"Fotokopi talebiniz reddedildi", "Gerekçe: " + r.getRejectReason(), link);
			case READY -> notifications.notify(List.of(r.getRequesterId()), "PRINT_READY", "Fotokopileriniz hazır",
					"%s teslim alınmaya hazır (%s).".formatted(r.getTitle(), queries.branchName(r.getBranchId())), link);
			case CANCELLED -> {
				if (from == PrintStatus.APPROVED && actorId != null) {
					notifications.notify(operators, "PRINT_CANCELLED", "Fotokopi talebi iptal edildi",
							"%s: %s".formatted(queries.fullName(r.getRequesterId()), masked), link);
				}
			}
			default -> {
			}
		}

		Set<Long> audience = new LinkedHashSet<>(operators);
		audience.add(r.getRequesterId());
		if (r.getApproverId() != null) {
			audience.add(r.getApproverId());
		}
		realtime.toUsers(audience, "PRINT_REQUEST_CHANGED", Map.of("requestId", r.getId(), "status", to.name()));
		realtime.toUsers(audience, "BADGES", Map.of());
	}

}
