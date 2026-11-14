package tr.okul.iletisim.core.group;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.common.util.TextNormalizer;

public final class GroupDtos {

	private static final String NAME_MESSAGE = "Ad 2–100 karakter olmalıdır.";
	private static final String DESCRIPTION_MESSAGE = "Açıklama en fazla 500 karakter olabilir.";

	private GroupDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record GroupRow(long id, String name, String description, int memberCount, boolean archived,
			OffsetDateTime createdAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record GroupMember(long userId, String fullName, String title, String branchName, String status) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record GroupDetail(long id, String name, String description, int memberCount, boolean archived,
			OffsetDateTime createdAt, List<GroupMember> members) {
	}

	public record GroupCreateRequest(
			@NotNull(message = NAME_MESSAGE) @Size(min = 2, max = 100, message = NAME_MESSAGE) String name,
			@Size(max = 500, message = DESCRIPTION_MESSAGE) String description,
			@NotNull(message = "Üye listesi gönderilmedi.") @Size(max = 1000, message = "Bir grupta en fazla 1000 üye olabilir.") List<Long> memberIds) {

		public GroupCreateRequest {
			name = TextNormalizer.clean(name);
			description = blankToNull(description);
		}

	}

	public record GroupUpdateRequest(
			@NotNull(message = NAME_MESSAGE) @Size(min = 2, max = 100, message = NAME_MESSAGE) String name,
			@Size(max = 500, message = DESCRIPTION_MESSAGE) String description) {

		public GroupUpdateRequest {
			name = TextNormalizer.clean(name);
			description = blankToNull(description);
		}

	}

	public record AddMembersRequest(
			@NotEmpty(message = "En az bir kişi seçin.") @Size(max = 500, message = "Bir seferde en fazla 500 kişi eklenebilir.") List<Long> userIds) {
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}
