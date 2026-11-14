package tr.okul.iletisim.print;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tr.okul.iletisim.common.file.FileStorageService;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.PageResponse;
import tr.okul.iletisim.print.PrintDtos.CreateDelegationRequest;
import tr.okul.iletisim.print.PrintDtos.CreatePrintRequest;
import tr.okul.iletisim.print.PrintDtos.DelegationDto;
import tr.okul.iletisim.print.PrintDtos.MonthlyReport;
import tr.okul.iletisim.print.PrintDtos.PageCountResponse;
import tr.okul.iletisim.print.PrintDtos.PrintContext;
import tr.okul.iletisim.print.PrintDtos.PrintRequestDetail;
import tr.okul.iletisim.print.PrintDtos.PrintRequestRow;
import tr.okul.iletisim.print.PrintDtos.PrintSeason;
import tr.okul.iletisim.print.PrintDtos.QueueBoard;
import tr.okul.iletisim.print.PrintDtos.RejectRequest;

@RestController
public class PrintController {

	private final PrintRequestService service;
	private final PrintDelegationService delegationService;
	private final PrintReportService reports;
	private final PrintQueries queries;
	private final PrintAccess access;
	private final FileStorageService files;

	public PrintController(PrintRequestService service, PrintDelegationService delegationService,
			PrintReportService reports, PrintQueries queries, PrintAccess access, FileStorageService files) {
		this.service = service;
		this.delegationService = delegationService;
		this.reports = reports;
		this.queries = queries;
		this.access = access;
		this.files = files;
	}

	@GetMapping("/api/print/context")
	public PrintContext context(UserContext me) {
		return service.context(me);
	}

	@GetMapping("/api/print/seasons")
	public List<PrintSeason> seasons(UserContext me) {
		me.requireMember();
		return queries.seasons(me.userId());
	}

	@GetMapping("/api/print/requests/mine")
	public PageResponse<PrintRequestRow> mine(UserContext me, @RequestParam(required = false) Long seasonId,
			@RequestParam(required = false) String group, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		me.requireMember();
		return queries.mine(me, seasonId, group, page, size);
	}

	@GetMapping("/api/print/approvals")
	public PageResponse<PrintRequestRow> approvals(UserContext me, @RequestParam(defaultValue = "pending") String tab,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		me.requireMember();
		return queries.approvals(me, tab, page, size);
	}

	@GetMapping("/api/print/queue/board")
	public QueueBoard board(UserContext me) {
		me.requireMember();
		access.requireOperator(me);
		return queries.board(me);
	}

	@GetMapping("/api/print/queue/delivered")
	public PageResponse<PrintRequestRow> delivered(UserContext me,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		me.requireMember();
		access.requireOperator(me);
		return queries.delivered(me, date, page, size);
	}

	@GetMapping("/api/print/requests/{id}")
	public PrintRequestDetail detail(UserContext me, @PathVariable long id) {
		return service.detail(me, id);
	}

	@GetMapping("/api/print/requests/{id}/file")
	public ResponseEntity<Resource> file(UserContext me, @PathVariable long id,
			@RequestParam(defaultValue = "false") boolean download) {
		return files.serve(service.file(me, id), !download);
	}

	@GetMapping("/api/print/files/{fileId}/page-count")
	public PageCountResponse pageCount(UserContext me, @PathVariable UUID fileId) {
		return new PageCountResponse(service.pageCount(me, fileId));
	}

	@PostMapping("/api/print/requests")
	@ResponseStatus(HttpStatus.CREATED)
	public PrintRequestDetail create(UserContext me, @Valid @RequestBody CreatePrintRequest request) {
		return service.create(me, request);
	}

	@PostMapping("/api/print/requests/{id}/cancel")
	public PrintRequestDetail cancel(UserContext me, @PathVariable long id) {
		return service.cancel(me, id);
	}

	@PostMapping("/api/print/requests/{id}/approve")
	public PrintRequestDetail approve(UserContext me, @PathVariable long id) {
		return service.approve(me, id);
	}

	@PostMapping("/api/print/requests/{id}/reject")
	public PrintRequestDetail reject(UserContext me, @PathVariable long id, @Valid @RequestBody RejectRequest request) {
		return service.reject(me, id, request.reason());
	}

	@PostMapping("/api/print/requests/{id}/start")
	public PrintRequestDetail start(UserContext me, @PathVariable long id) {
		return service.start(me, id);
	}

	@PostMapping("/api/print/requests/{id}/ready")
	public PrintRequestDetail ready(UserContext me, @PathVariable long id) {
		return service.ready(me, id);
	}

	@PostMapping("/api/print/requests/{id}/deliver")
	public PrintRequestDetail deliver(UserContext me, @PathVariable long id) {
		return service.deliver(me, id);
	}

	@GetMapping("/api/print/delegations")
	public List<DelegationDto> delegations(UserContext me) {
		return delegationService.list(me);
	}

	@PostMapping("/api/print/delegations")
	@ResponseStatus(HttpStatus.CREATED)
	public DelegationDto createDelegation(UserContext me, @Valid @RequestBody CreateDelegationRequest request) {
		return delegationService.create(me, request);
	}

	@DeleteMapping("/api/print/delegations/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDelegation(UserContext me, @PathVariable long id) {
		delegationService.delete(me, id);
	}

	@GetMapping("/api/print/reports/monthly")
	public MonthlyReport monthly(UserContext me, @RequestParam String month,
			@RequestParam(required = false) Long branchId) {
		return reports.monthly(me, month, branchId);
	}

	@GetMapping("/api/print/reports/monthly.csv")
	public ResponseEntity<byte[]> monthlyCsv(UserContext me, @RequestParam String month,
			@RequestParam(required = false) Long branchId) {
		byte[] body = reports.monthlyCsv(me, month, branchId);
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename("fotokopi-raporu-" + month + ".csv", StandardCharsets.UTF_8).build();
		return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(body);
	}

}
