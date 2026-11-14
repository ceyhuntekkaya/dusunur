package tr.okul.iletisim.common.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Formda düzeltilmesi gereken alanlar var."),
	REQUEST_UNREADABLE(HttpStatus.BAD_REQUEST, "Gönderilen bilgiler okunamadı. Sayfayı yenileyip tekrar deneyin."),
	REQUEST_INVALID_PARAM(HttpStatus.BAD_REQUEST, "Geçersiz istek parametresi: {0}."),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Oturumunuz sona erdi. Lütfen tekrar giriş yapın."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok."),
	CSRF_INVALID(HttpStatus.FORBIDDEN, "Güvenlik doğrulaması başarısız oldu. Sayfayı yenileyip tekrar deneyin."),
	MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN,
			"Bu bölümü kullanabilmek için bu eğitim-öğretim yılına atanmış olmanız gerekir."),
	KVKK_REQUIRED(HttpStatus.FORBIDDEN, "Devam etmek için aydınlatma metnini onaylamanız gerekiyor."),
	ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "İstenen adres bulunamadı."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Aradığınız kayıt bulunamadı veya silinmiş."),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT,
			"Bu kayıt siz işlem yaparken başka biri tarafından değiştirildi. Sayfayı yenileyip tekrar deneyin."),
	DUPLICATE_RECORD(HttpStatus.CONFLICT, "Bu bilgilerle kayıtlı başka bir kayıt var."),
	RECORD_IN_USE(HttpStatus.CONFLICT,
			"Bu kayıt başka kayıtlarla ilişkili olduğu için işlem yapılamıyor."),
	SEASON_NONE_ACTIVE(HttpStatus.CONFLICT,
			"Şu anda aktif bir eğitim-öğretim yılı yok. Bu işlem yapılamıyor."),
	SEASON_READ_ONLY(HttpStatus.CONFLICT,
			"Bu kayıt geçmiş bir eğitim-öğretim yılına ait. Görüntüleyebilirsiniz ancak değiştiremezsiniz."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla deneme yaptınız. Lütfen {0} dakika sonra tekrar deneyin."),
	FILE_MISSING(HttpStatus.BAD_REQUEST, "Dosya seçilmedi."),
	FILE_EMPTY(HttpStatus.BAD_REQUEST, "Seçilen dosya boş. Lütfen başka bir dosya seçin."),
	FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya boyutu en fazla {0} MB olabilir."),
	FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Bu dosya türü yüklenemez. İzin verilen türler: {0}."),
	FILE_CONTENT_MISMATCH(HttpStatus.BAD_REQUEST,
			"Dosyanın içeriği uzantısıyla uyuşmuyor. Dosyayı kontrol edip tekrar deneyin."),
	FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Dosya bulunamadı veya silinmiş."),
	FILE_NOT_USABLE(HttpStatus.BAD_REQUEST, "Bu dosya kullanılamıyor. Dosyayı yeniden yükleyin."),
	FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
			"Dosya kaydedilemedi. Lütfen tekrar deneyin; sorun sürerse sistem yöneticisine bildirin."),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,
			"Sistem şu anda yanıt veremiyor. Birkaç dakika sonra tekrar deneyin."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
			"Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin. Sorun devam ederse sistem yöneticisine şu kodu iletin: {0}");

	private final HttpStatus status;
	private final String message;

	CommonErrorCode(HttpStatus status, String message) {
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
