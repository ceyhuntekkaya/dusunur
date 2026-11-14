package tr.okul.iletisim.print;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PrintDtos {

	private static final String TITLE_MESSAGE = "Açıklama 3–200 karakter olmalıdır.";
	private static final String PAGE_MESSAGE = "Sayfa sayısı 1 ile 2000 arasında olmalıdır.";
	private static final String COPIES_MESSAGE = "Kopya adedi 1 ile 1000 arasında olmalıdır.";
	private static final String CHOICE_MESSAGE = "Bir seçenek seçin.";

	private PrintDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PersonRef(long id, String fullName, String branchName) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PrintRequestRow(long id, long seasonId, boolean archived, String title, boolean secret,
			PrintStatus status, PersonRef requester, String branchName, int pageCount, int copies, int totalPages,
			PrintColor color, PrintSides sides, PaperSize paperSize, boolean originalOnPaper, OffsetDateTime dueAt,
			OffsetDateTime createdAt, PersonRef approver) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PrintHistoryItem(PrintStatus fromStatus, PrintStatus toStatus, PersonRef actor, String note,
			OffsetDateTime createdAt) {
	}

	public record PrintFileInfo(String name, String contentType, long sizeBytes, boolean purged) {
	}

	public record PrintActions(boolean canCancel, boolean canCopy, boolean canApprove, boolean canReject,
			boolean canStart, boolean canMarkReady, boolean canDeliver, boolean canDownload) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PrintRequestDetail(long id, long seasonId, boolean archived, String title, boolean secret,
			PrintStatus status, PersonRef requester, String branchName, int pageCount, int copies, int totalPages,
			PrintColor color, PrintSides sides, PaperSize paperSize, boolean originalOnPaper, OffsetDateTime dueAt,
			OffsetDateTime createdAt, PersonRef approver,
			String note, String rejectReason, String cancelReason, PersonRef originalApprover, PersonRef decidedBy,
			OffsetDateTime decidedAt, PersonRef operator, OffsetDateTime startedAt, OffsetDateTime readyAt,
			OffsetDateTime deliveredAt, OffsetDateTime cancelledAt, PrintFileInfo file, List<PrintHistoryItem> history,
			PrintActions actions) {
	}

	public record CreatePrintRequest(
			@NotNull(message = TITLE_MESSAGE) @Pattern(regexp = "(?s)^\\s*\\S.{1,198}\\S\\s*$", message = TITLE_MESSAGE) String title,
			UUID fileId,
			boolean originalOnPaper,
			@NotNull(message = PAGE_MESSAGE) @Min(value = 1, message = PAGE_MESSAGE) @Max(value = 2000, message = PAGE_MESSAGE) Integer pageCount,
			@NotNull(message = COPIES_MESSAGE) @Min(value = 1, message = COPIES_MESSAGE) @Max(value = 1000, message = COPIES_MESSAGE) Integer copies,
			@NotNull(message = CHOICE_MESSAGE) PrintColor color,
			@NotNull(message = CHOICE_MESSAGE) PrintSides sides,
			@NotNull(message = CHOICE_MESSAGE) PaperSize paperSize,
			boolean secret,
			@NotNull(message = "Teslim tarihi ve saatini seçin.") OffsetDateTime dueAt,
			@Size(max = 500, message = "Not en fazla 500 karakter olabilir.") String note,
			Long copiedFromId) {
	}

	public record RejectRequest(
			@NotNull(message = "Red gerekçesi 5–500 karakter olmalıdır.") @Pattern(regexp = "(?s)^\\s*\\S.{3,498}\\S\\s*$", message = "Red gerekçesi 5–500 karakter olmalıdır.") String reason) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record OnLeave(PersonRef approver, PersonRef delegate, LocalDate endDate) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record PrintContext(boolean isApprover, boolean isOperator, boolean isManager, String branchName,
			PersonRef myApprover, OnLeave myApproverOnLeave, int minLeadMinutes, int maxDaysAhead) {
	}

	public record PrintSeason(long id, String name, boolean active) {
	}

	public record DelegationDto(long id, PersonRef delegate, LocalDate startDate, LocalDate endDate, boolean active) {
	}

	public record CreateDelegationRequest(
			@NotNull(message = "Vekil seçin.") Long delegateId,
			@NotNull(message = "Başlangıç tarihi seçin.") LocalDate startDate,
			@NotNull(message = "Bitiş tarihi seçin.") LocalDate endDate) {
	}

	public record QueueBoard(List<PrintRequestRow> approved, List<PrintRequestRow> inProgress,
			List<PrintRequestRow> ready) {
	}

	public record ReportRow(long userId, String fullName, String branchName, long requestCount, long bwPages,
			long colorPages, long totalPages) {
	}

	public record ReportTotals(long requestCount, long bwPages, long colorPages, long totalPages) {
	}

	public record MonthlyReport(String month, List<ReportRow> rows, ReportTotals totals) {
	}

	public record PageCountResponse(Integer pageCount) {
	}

}
