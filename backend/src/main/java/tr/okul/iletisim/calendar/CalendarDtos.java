package tr.okul.iletisim.calendar;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetRef;
import tr.okul.iletisim.core.directory.DirectoryDtos.TargetView;

public final class CalendarDtos {

	private static final String CAL_NAME_MESSAGE = "Takvim adı 2–100 karakter olmalıdır.";
	private static final String TITLE_MESSAGE = "Başlık 2–150 karakter olmalıdır.";

	private CalendarDtos() {
	}

	public record CalPerson(long id, String fullName) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CalendarDto(long id, CalendarType type, String name, String description, String color,
			CalendarSection section, CalAccess myAccess, boolean canCreate, boolean canManage, boolean readOnly,
			boolean isDefault, String branchName, CalPerson owner) {
	}

	public record EntryFile(String name, String contentType, long sizeBytes) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EntryDto(long id, long calendarId, String title, boolean allDay, OffsetDateTime startAt,
			OffsetDateTime endAt, LocalDate startDate, LocalDate endDate, String location, String description,
			EntryFile file, CalPerson createdBy, OffsetDateTime createdAt, OffsetDateTime updatedAt,
			boolean canEdit) {
	}

	public record EntryInput(Long calendarId,
			@NotNull(message = TITLE_MESSAGE) @Size(min = 2, max = 150, message = TITLE_MESSAGE) String title,
			boolean allDay, LocalDate startDate, LocalDate endDate, OffsetDateTime startAt, OffsetDateTime endAt,
			@Size(max = 200, message = "Konum en fazla 200 karakter olabilir.") String location,
			@Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir.") String description,
			UUID fileId, Boolean removeFile) {
	}

	public record CalendarRequest(
			@NotNull(message = CAL_NAME_MESSAGE) @Size(min = 2, max = 100, message = CAL_NAME_MESSAGE) String name,
			@Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.") String description,
			Long branchId) {
	}

	public record CalShareRow(long id, TargetView subject, CalShareLevel level) {
	}

	public record ShareResult(CalShareRow row, boolean created) {
	}

	public record ShareCreateRequest(@NotNull(message = "Bir kişi veya grup seçin.") @Valid TargetRef subject,
			@NotNull(message = "Bir yetki seviyesi seçin.") CalShareLevel level) {
	}

	public record ShareUpdateRequest(@NotNull(message = "Bir yetki seviyesi seçin.") CalShareLevel level) {
	}

}
