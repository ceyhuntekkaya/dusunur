package tr.okul.iletisim.calendar;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@Table(name = "cal_entry")
@Getter
@Setter
public class CalEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "calendar_id")
	private Long calendarId;

	@Column(name = "is_resource")
	private boolean resource;

	private String title;

	@Column(name = "all_day")
	private boolean allDay;

	@Column(name = "start_at")
	private OffsetDateTime startAt;

	@Column(name = "end_at")
	private OffsetDateTime endAt;

	private String location;

	private String description;

	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

}
