package tr.okul.iletisim.dms;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum DmsErrorCode implements ErrorCode {

	DMS_SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Doküman alanı bulunamadı veya bu alana erişiminiz yok."),
	DMS_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Klasör veya dosya bulunamadı ya da silinmiş."),
	DMS_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "Doküman alanı açma yetkiniz yok. Okul yönetimiyle iletişime geçin."),
	DMS_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu alanda değişiklik yapma yetkiniz yok (yetkiniz: {0})."),
	DMS_MANAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Bu işlem için alanda \"Yönetim\" yetkisi gerekir."),
	DMS_SPACE_NAME_TAKEN(HttpStatus.CONFLICT, "Bu adla bir doküman alanı zaten var."),
	DMS_SPACE_NOT_EMPTY(HttpStatus.CONFLICT, "Alanı silmek için önce içindeki tüm klasör ve dosyaları silin ve çöp kutusunu boşaltın."),
	DMS_GRANT_OWNER(HttpStatus.BAD_REQUEST, "Alanın sahibi zaten tam yetkilidir."),
	DMS_GRANT_LIMIT(HttpStatus.BAD_REQUEST, "Bir alanda en fazla 100 yetki tanımlanabilir. Kişiler yerine grup veya şube seçmeyi deneyin."),
	DMS_NAME_INVALID(HttpStatus.BAD_REQUEST, "Ad boş olamaz, çok uzun olamaz ve şu karakterleri içeremez: / \\ : * ? \" < > |"),
	DMS_NAME_EXISTS(HttpStatus.CONFLICT, "Bu konumda \"{0}\" adında bir öğe zaten var. Başka bir ad seçin."),
	DMS_NAME_CONFLICT(HttpStatus.CONFLICT, "\"{0}\" adında bir dosya zaten var."),
	DMS_EXTENSION_CHANGE(HttpStatus.BAD_REQUEST, "Dosya uzantısı değiştirilemez."),
	DMS_DEPTH_LIMIT(HttpStatus.BAD_REQUEST, "Klasörler en fazla 10 kat iç içe olabilir."),
	DMS_MOVE_INVALID(HttpStatus.BAD_REQUEST, "Öğe bu konuma taşınamaz. Bir klasör kendi içine veya alt klasörüne taşınamaz.");

	private final HttpStatus status;
	private final String message;

	DmsErrorCode(HttpStatus status, String message) {
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
