package tr.okul.iletisim.core.season;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.user.UserDtos;
import tr.okul.iletisim.core.user.UserDtos.MembershipInput;

public final class SeasonDtos {

	private static final String NAME_MESSAGE = "Yıl adı 2026-2027 biçiminde olmalı ve ikinci yıl birinciden bir fazla olmalıdır.";
	private static final String IDS_MESSAGE = "En az bir kayıt seçin.";
	private static final String IDS_MAX_MESSAGE = "Bir seferde en fazla 500 kayıt seçilebilir.";

	private SeasonDtos() {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SeasonRow(long id, String name, LocalDate startDate, LocalDate endDate, String status, int memberCount,
			OffsetDateTime activatedAt, OffsetDateTime closedAt) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record MembershipRow(long id, long branchId, String branchName, String title, boolean manager,
			boolean printOperator, boolean canCreateDmsSpace, Long printApproverId, String printApproverName,
			long userId, String fullName, String email, String userStatus) {
	}

	public record SeasonRequest(
			@NotNull(message = NAME_MESSAGE) String name,
			@NotNull(message = "Başlangıç tarihini seçin.") LocalDate startDate,
			@NotNull(message = "Bitiş tarihini seçin.") LocalDate endDate) {

		public SeasonRequest {
			name = TextNormalizer.clean(name);
		}

	}

	public record AddMembershipRequest(
			@NotNull(message = "Personel seçin.") Long userId,
			@NotNull(message = "Şube seçin.") Long branchId,
			@Size(max = 100, message = UserDtos.TITLE_MESSAGE) String title,
			boolean manager, boolean printOperator, boolean canCreateDmsSpace, Long printApproverId) {

		public MembershipInput toInput() {
			return new MembershipInput(branchId, title, manager, printOperator, canCreateDmsSpace, printApproverId);
		}

	}

	public record BatchRequest(
			@NotEmpty(message = IDS_MESSAGE) @Size(max = 500, message = IDS_MAX_MESSAGE) List<Long> userIds,
			@NotNull(message = "Şube seçin.") Long branchId) {
	}

	public record BatchResponse(int created) {
	}

	public record BulkUpdateRequest(
			@NotEmpty(message = IDS_MESSAGE) @Size(max = 500, message = IDS_MAX_MESSAGE) List<Long> membershipIds,
			Long branchId, Long printApproverId, Boolean clearApprover) {
	}

	public record BulkUpdateResponse(int updated, int skippedSelf) {
	}

	public record BulkDeleteRequest(
			@NotEmpty(message = IDS_MESSAGE) @Size(max = 500, message = IDS_MAX_MESSAGE) List<Long> membershipIds) {
	}

	public record BulkDeleteResponse(int deleted) {
	}

	public record CopyRequest(
			@NotNull(message = "Kaynak yılı seçin.") Long sourceSeasonId,
			@NotEmpty(message = IDS_MESSAGE) @Size(max = 1000, message = "Bir seferde en fazla 1000 kişi seçilebilir.") List<Long> userIds) {
	}

	public record CopyResponse(int created, int skippedExisting, int approverCleared) {
	}

	public record ActivationPreview(boolean canActivate, List<String> blockers, List<String> items) {
	}

}
