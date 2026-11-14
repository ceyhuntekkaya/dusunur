package tr.okul.iletisim.dms;

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
@Table(name = "dms_space_grant")
@Getter
@Setter
public class DmsSpaceGrant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "space_id")
	private Long spaceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "subject_type")
	private TargetType subjectType;

	@Column(name = "subject_id")
	private Long subjectId;

	@Convert(converter = DmsLevelConverter.class)
	private DmsLevel level;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

}
