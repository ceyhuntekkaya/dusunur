package tr.okul.iletisim.core.season;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "season_membership")
@Getter
@Setter
public class SeasonMembership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "season_id")
	private Long seasonId;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "branch_id")
	private Long branchId;

	private String title;

	@Column(name = "is_manager")
	private boolean manager;

	@Column(name = "is_print_operator")
	private boolean printOperator;

	@Column(name = "can_create_dms_space")
	private boolean canCreateDmsSpace;

	@Column(name = "print_approver_id")
	private Long printApproverId;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

}
