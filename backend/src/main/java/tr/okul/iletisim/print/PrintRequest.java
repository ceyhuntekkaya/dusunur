package tr.okul.iletisim.print;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "print_request")
@Getter
@Setter
public class PrintRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "season_id")
	private Long seasonId;

	@Column(name = "requester_id")
	private Long requesterId;

	@Column(name = "branch_id")
	private Long branchId;

	private String title;

	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "original_on_paper")
	private boolean originalOnPaper;

	@Column(name = "page_count")
	private int pageCount;

	private int copies;

	@Enumerated(EnumType.STRING)
	private PrintColor color;

	@Enumerated(EnumType.STRING)
	private PrintSides sides;

	@Enumerated(EnumType.STRING)
	@Column(name = "paper_size")
	private PaperSize paperSize;

	private boolean secret;

	@Column(name = "due_at")
	private OffsetDateTime dueAt;

	private String note;

	@Column(name = "total_pages")
	private int totalPages;

	@Enumerated(EnumType.STRING)
	private PrintStatus status;

	@Column(name = "approver_id")
	private Long approverId;

	@Column(name = "original_approver_id")
	private Long originalApproverId;

	@Column(name = "decided_by")
	private Long decidedBy;

	@Column(name = "decided_at")
	private OffsetDateTime decidedAt;

	@Column(name = "reject_reason")
	private String rejectReason;

	@Column(name = "operator_id")
	private Long operatorId;

	@Column(name = "started_at")
	private OffsetDateTime startedAt;

	@Column(name = "ready_at")
	private OffsetDateTime readyAt;

	@Column(name = "delivered_at")
	private OffsetDateTime deliveredAt;

	@Column(name = "cancelled_by")
	private Long cancelledBy;

	@Column(name = "cancelled_at")
	private OffsetDateTime cancelledAt;

	@Column(name = "cancel_reason")
	private String cancelReason;

	@Column(name = "file_purged_at")
	private OffsetDateTime filePurgedAt;

	@Column(name = "copied_from_id")
	private Long copiedFromId;

	@Version
	private int version;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

}
