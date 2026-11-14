package tr.okul.iletisim.print;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum PrintErrorCode implements ErrorCode {

	PRINT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Fotokopi talebi bulunamadı veya görüntüleme yetkiniz yok."),
	PRINT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "Bir dosya yükleyin veya \"Basılı orijinali getireceğim\" seçeneğini işaretleyin."),
	PRINT_TOTAL_TOO_LARGE(HttpStatus.BAD_REQUEST, "Toplam baskı (sayfa × kopya) en fazla 20.000 olabilir."),
	PRINT_DUE_TOO_SOON(HttpStatus.BAD_REQUEST, "Teslim zamanı en erken {0} dakika sonrası olabilir. Lütfen daha ileri bir saat seçin."),
	PRINT_DUE_TOO_LATE(HttpStatus.BAD_REQUEST, "Teslim zamanı en fazla {0} gün sonrası olabilir."),
	PRINT_INVALID_STATUS(HttpStatus.CONFLICT, "Bu işlem yapılamıyor; talebin şu anki durumu: {0}. Sayfayı yenileyip tekrar deneyin."),
	PRINT_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "Talep yalnızca hazırlanmaya başlanmadan önce, talep sahibi tarafından iptal edilebilir."),
	PRINT_APPROVE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu talebi yalnızca atanan onaylayıcı onaylayabilir veya reddedebilir."),
	PRINT_OPERATOR_ONLY(HttpStatus.FORBIDDEN, "Bu işlem yalnızca ilgili şubenin fotokopi sorumlusu tarafından yapılabilir."),
	PRINT_NO_FILE(HttpStatus.NOT_FOUND, "Bu talepte dosya yok; basılı orijinal fotokopi birimine getirilecek."),
	PRINT_FILE_PURGED(HttpStatus.GONE, "Dosya saklama süresi dolduğu için silindi."),
	PRINT_REPORT_FORBIDDEN(HttpStatus.FORBIDDEN, "Fotokopi raporunu yalnızca yöneticiler görüntüleyebilir."),
	PRINT_REPORT_MONTH_INVALID(HttpStatus.BAD_REQUEST, "Geçerli bir ay seçin."),
	PRINT_DELEGATE_INVALID(HttpStatus.BAD_REQUEST, "Vekil, bu yıla atanmış aktif bir personel olmalı ve siz olmamalısınız."),
	PRINT_DELEGATION_DATES_INVALID(HttpStatus.BAD_REQUEST, "Başlangıç bugün veya sonrası, bitiş başlangıçtan sonra olmalı; vekâlet en fazla 90 gün sürebilir."),
	PRINT_DELEGATION_OVERLAP(HttpStatus.CONFLICT, "Bu tarihlerle çakışan bir vekâletiniz var. Önce onu silin."),
	PRINT_DELEGATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Vekâlet kaydı bulunamadı.");

	private final HttpStatus status;
	private final String message;

	PrintErrorCode(HttpStatus status, String message) {
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
