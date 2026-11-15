package tr.okul.iletisim.core.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	public static final String PASSWORD_MESSAGE = "Şifre en az 8 karakter olmalı, en az bir harf ve bir rakam içermelidir.";
	public static final String PASSWORD_REGEX = "^(?=.*\\p{L})(?=.*\\d).{8,64}$";

	private AuthDtos() {
	}

	public record LoginRequest(
			@NotBlank(message = "E-posta adresinizi girin.") @Size(max = 254, message = "E-posta adresi çok uzun.") String email,
			@NotBlank(message = "Şifrenizi girin.") @Size(max = 200, message = "Şifre çok uzun.") String password) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record MeResponse(long id, String firstName, String lastName, String fullName, String email, String phone,
			String discipline, boolean systemAdmin, boolean kvkkRequired, Season season, Membership membership,
			Permissions permissions) {

		public record Season(long id, String name) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record Membership(long branchId, String branchName, String title, boolean manager,
				boolean printOperator, boolean canCreateDmsSpace, Long printApproverId, String printApproverName) {
		}

		public record Permissions(boolean member, boolean canPostToAll, boolean canPostImportant,
				boolean canManageGroups, boolean canEditInstitutionCalendar, boolean canCreateDmsSpace,
				boolean printOperator, boolean canAccessAdmin) {
		}

	}

	public record KvkkResponse(int version, String text) {
	}

	public record ChangePasswordRequest(
			@NotBlank(message = "Mevcut şifrenizi girin.") String currentPassword,
			@NotNull(message = PASSWORD_MESSAGE) @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {
	}

	public record ForgotPasswordRequest(
			@NotBlank(message = "E-posta adresinizi girin.") @Size(max = 254, message = "E-posta adresi çok uzun.") String email) {
	}

	public record MessageResponse(String message) {
	}

	public record TokenInfoResponse(String purpose, String firstName, String email) {
	}

	public record SetPasswordRequest(
			@NotBlank(message = "Bağlantı geçersiz.") String token,
			@NotNull(message = PASSWORD_MESSAGE) @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {
	}

}
