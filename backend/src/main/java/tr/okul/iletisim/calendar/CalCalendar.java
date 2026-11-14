package tr.okul.iletisim.calendar;

import java.time.OffsetDateTime;

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
import lombok.Getter;
import lombok.Setter;
import tr.okul.iletisim.common.util.TextNormalizer;

@Entity
@Table(name = "cal_calendar")
@Getter
@Setter
public class CalCalendar {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private CalendarType type;

	private String name;

	@Column(name = "name_key")
	private String nameKey;

	private String description;

	@Column(name = "owner_id")
	private Long ownerId;

	@Column(name = "branch_id")
	private Long branchId;

	@Column(name = "staff_group_id")
	private Long staffGroupId;

	@Column(name = "is_default")
	private boolean isDefault;

	@Column(name = "read_only")
	private boolean readOnly;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	public void setName(String name) {
		this.name = name;
		this.nameKey = TextNormalizer.fold(name);
	}

}
