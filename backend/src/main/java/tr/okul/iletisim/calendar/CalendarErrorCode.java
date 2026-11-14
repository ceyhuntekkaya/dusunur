package tr.okul.iletisim.calendar;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum CalendarErrorCode implements ErrorCode {

	CAL_CALENDAR_NOT_FOUND(HttpStatus.NOT_FOUND, "Takvim bulunamadı veya erişiminiz yok."),
	CAL_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Kayıt bulunamadı veya silinmiş."),
	CAL_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu takvime kayıt ekleme yetkiniz yok."),
	CAL_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu kaydı yalnızca ekleyen kişi veya takvimi yönetmeye yetkili kişiler değiştirebilir."),
	CAL_READ_ONLY(HttpStatus.CONFLICT, "Bu takvim salt okunur; grup arşivlendiği için değişiklik yapılamaz."),
	CAL_MANAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu takvimi yönetme yetkiniz yok."),
	CAL_SYSTEM_CALENDAR(HttpStatus.BAD_REQUEST, "Bu takvim sistem tarafından yönetilir; adı değiştirilemez ve silinemez."),
	CAL_DEFAULT_CALENDAR(HttpStatus.BAD_REQUEST, "Varsayılan kişisel takviminiz silinemez ve adı değiştirilemez."),
	CAL_PERSONAL_LIMIT(HttpStatus.BAD_REQUEST, "En fazla 10 kişisel takviminiz olabilir."),
	CAL_RESOURCE_FORBIDDEN(HttpStatus.FORBIDDEN, "Salon ve laboratuvarları yalnızca yöneticiler tanımlayabilir."),
	CAL_RESOURCE_NAME_TAKEN(HttpStatus.CONFLICT, "Bu adla bir salon veya laboratuvar zaten var."),
	CAL_RESOURCE_HAS_FUTURE(HttpStatus.CONFLICT, "Bu salonun ileri tarihli {0} rezervasyonu var. Silmeden önce bu rezervasyonları kaldırın."),
	CAL_RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Bu salon seçilen saatte dolu: \"{0}\" ({1}, {2}). Lütfen başka bir saat seçin."),
	CAL_END_BEFORE_START(HttpStatus.BAD_REQUEST, "Bitiş, başlangıçtan sonra olmalıdır."),
	CAL_DURATION_TOO_LONG(HttpStatus.BAD_REQUEST, "Bir kayıt en fazla 31 gün sürebilir."),
	CAL_DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Geçerli bir tarih seçin."),
	CAL_RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "Tek seferde en fazla 100 günlük kayıt görüntülenebilir."),
	CAL_SHARE_NOT_PERSONAL(HttpStatus.BAD_REQUEST, "Yalnızca kişisel takvimler paylaşılabilir."),
	CAL_SHARE_OWNER(HttpStatus.BAD_REQUEST, "Takvimi kendinizle paylaşamazsınız."),
	CAL_SHARE_LIMIT(HttpStatus.BAD_REQUEST, "Bir takvim en fazla 50 kişi veya grupla paylaşılabilir.");

	private final HttpStatus status;
	private final String message;

	CalendarErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}

}
