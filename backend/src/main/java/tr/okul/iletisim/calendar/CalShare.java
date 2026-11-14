package tr.okul.iletisim.calendar;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tr.okul.iletisim.core.directory.TargetType;

@Entity
@Table(name = "cal_share")
@Getter
@Setter
public class CalShare {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "calendar_id")
	private Long calendarId;

	@Enumerated(EnumType.STRING)
	@Column(name = "subject_type")
	private TargetType subjectType;

	@Column(name = "subject_id")
	private Long subjectId;

	@Convert(converter = CalShareLevelConverter.class)
	private CalShareLevel level;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

}
