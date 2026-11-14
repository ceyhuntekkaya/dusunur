package tr.okul.iletisim.common.audit;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonRawValue;

public final class AuditDtos {

	private AuditDtos() {
	}

	public record AuditRow(long id, OffsetDateTime createdAt, Long actorId, String actorName, String ip,
			String action, String actionLabel, String entityType, String entityId, @JsonRawValue String detail) {
	}

	public record AuditActionOption(String code, String label) {
	}

}
