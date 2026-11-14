package tr.okul.iletisim.core.user;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.spi.ImpactItem;

public final class UserDtos {

	public static final String FIRST_NAME_MESSAGE = "Ad 2–50 karakter olmalı ve yalnızca harf içermelidir.";
	public static final String LAST_NAME_MESSAGE = "Soyad 2–50 karakter olmalı ve yalnızca harf içermelidir.";
	public static final String EMAIL_MESSAGE = "Geçerli bir e-posta adresi girin.";
	public static final String TITLE_MESSAGE = "Unvan en fazla 100 karakter olabilir.";
	public static final String DISCIPLINE_MESSAGE = "Branş en fazla 100 karakter olabilir.";
	public static final String NAME_REGEX = "^[\\p{L}][\\p{L} .'-]*$";
	public static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

	private UserDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record MembershipInfo(long id, long branchId, String branchName, String title, boolean manager,
			boolean printOperator, boolean canCreateDmsSpace, Long printApproverId, String printApproverName) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record AdminUserRow(long id, String firstName, String lastName, String fullName, String email, String phone,
			String discipline, String status, boolean systemAdmin, boolean passwordSet, OffsetDateTime lastLoginAt,
			MembershipInfo membership) {
	}

	public record MembershipInput(
			@NotNull(message = "Şube seçin.") Long branchId,
			@Size(max = 100, message = TITLE_MESSAGE) String title,
			boolean manager, boolean printOperator, boolean canCreateDmsSpace, Long printApproverId) {

		public MembershipInput {
			title = blankToNull(TextNormalizer.clean(title));
		}

	}

	public record UserInput(
			@NotNull(message = FIRST_NAME_MESSAGE) @Size(min = 2, max = 50, message = FIRST_NAME_MESSAGE) @Pattern(regexp = NAME_REGEX, message = FIRST_NAME_MESSAGE) String firstName,
			@NotNull(message = LAST_NAME_MESSAGE) @Size(min = 2, max = 50, message = LAST_NAME_MESSAGE) @Pattern(regexp = NAME_REGEX, message = LAST_NAME_MESSAGE) String lastName,
			@NotNull(message = EMAIL_MESSAGE) @Size(max = 254, message = EMAIL_MESSAGE) @Pattern(regexp = EMAIL_REGEX, message = EMAIL_MESSAGE) String email,
			@ValidPhone String phone,
			@Size(max = 100, message = DISCIPLINE_MESSAGE) String discipline,
			boolean systemAdmin,
			@Valid MembershipInput membership) {

		public UserInput {
			firstName = TextNormalizer.clean(firstName);
			lastName = TextNormalizer.clean(lastName);
			email = TextNormalizer.lowerEmail(email);
			phone = blankToNull(phone);
			discipline = blankToNull(TextNormalizer.clean(discipline));
		}

	}

	public record CreateUserResponse(AdminUserRow user, boolean mailSent) {
	}

	public record DeactivationCheck(List<ImpactItem> items, boolean successorRequired) {
	}

	public record DeactivateRequest(Long successorUserId) {
	}

	public record ImportResponse(int created) {
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

}
