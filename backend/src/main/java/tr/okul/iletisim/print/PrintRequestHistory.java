package tr.okul.iletisim.print;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

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

@Entity
@Table(name = "print_request_history")
@Getter
@Setter
public class PrintRequestHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_id")
	private Long requestId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private PrintStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status")
	private PrintStatus toStatus;

	@Column(name = "actor_id")
	private Long actorId;

	private String note;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

}
