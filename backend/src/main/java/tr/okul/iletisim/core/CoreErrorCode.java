package tr.okul.iletisim.core;

import org.springframework.http.HttpStatus;

import tr.okul.iletisim.common.error.ErrorCode;

public enum CoreErrorCode implements ErrorCode {

	AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E-posta adresi veya şifre hatalı."),
	AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Çok sayıda hatalı giriş denemesi yapıldı. Lütfen {0} dakika sonra tekrar deneyin veya \"Şifremi unuttum\" bağlantısını kullanın."),
	AUTH_PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "Hesabınız için henüz şifre belirlenmemiş. E-postanıza gönderilen bağlantıyla şifrenizi belirleyin ya da \"Şifremi unuttum\" bağlantısını kullanın."),
	AUTH_ACCOUNT_PASSIVE(HttpStatus.UNAUTHORIZED, "Hesabınız pasif durumda. Okul yönetimiyle iletişime geçin."),
	AUTH_NO_ACTIVE_SEASON(HttpStatus.FORBIDDEN, "Şu anda aktif bir eğitim-öğretim yılı bulunmuyor. Okul yönetimiyle iletişime geçin."),
	AUTH_NO_SEASON_MEMBERSHIP(HttpStatus.FORBIDDEN, "{0} eğitim-öğretim yılına atanmamışsınız. Okul yönetimiyle iletişime geçin."),
	AUTH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Bu bağlantı geçersiz veya süresi dolmuş. \"Şifremi unuttum\" sayfasından yeni bir bağlantı isteyin."),
	AUTH_CURRENT_PASSWORD_WRONG(HttpStatus.BAD_REQUEST, "Mevcut şifreniz hatalı."),
	AUTH_PASSWORD_SAME(HttpStatus.BAD_REQUEST, "Yeni şifre mevcut şifrenizle aynı olamaz."),
	CORE_ADMIN_ONLY(HttpStatus.FORBIDDEN, "Bu işlem yalnızca sistem yöneticisi tarafından yapılabilir."),
	CORE_MANAGER_ONLY(HttpStatus.FORBIDDEN, "Bu işlem yalnızca yöneticiler tarafından yapılabilir."),
	PUSH_SUBSCRIPTION_INVALID(HttpStatus.BAD_REQUEST, "Bildirim aboneliği kaydedilemedi. Sayfayı yenileyip tekrar deneyin."),
	MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "E-posta gönderilemedi. E-posta sunucusuna ulaşılamıyor; birkaç dakika sonra tekrar deneyin."),
	BULK_NOTHING_TO_UPDATE(HttpStatus.BAD_REQUEST, "Değiştirilecek bir bilgi seçin."),
	BULK_CONFLICT(HttpStatus.BAD_REQUEST, "Aynı anda hem onaylayıcı atayıp hem kaldıramazsınız."),
	AUDIT_RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "Tarih aralığı en fazla 1 yıl olabilir."),
	TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "En az bir alıcı seçin."),
	TARGET_TYPE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "\"{0}\" alıcısını seçme yetkiniz yok."),
	TARGET_INVALID(HttpStatus.BAD_REQUEST, "Seçilen alıcılardan biri artık geçerli değil (pasif, arşivlenmiş ya da silinmiş olabilir). Alıcıları yeniden seçin."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Personel bulunamadı."),
	USER_EMAIL_TAKEN(HttpStatus.CONFLICT, "Bu e-posta adresiyle kayıtlı bir personel var."),
	USER_PASSIVE(HttpStatus.BAD_REQUEST, "Pasif personele bu işlem yapılamaz."),
	USER_ALREADY_PASSIVE(HttpStatus.CONFLICT, "Bu personel zaten pasif."),
	USER_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Bu personel zaten aktif."),
	USER_CANNOT_DEACTIVATE_SELF(HttpStatus.BAD_REQUEST, "Kendi hesabınızı pasifleştiremezsiniz."),
	USER_CANNOT_REMOVE_OWN_ADMIN(HttpStatus.BAD_REQUEST, "Kendi sistem yöneticisi yetkinizi kaldıramazsınız."),
	USER_LAST_ADMIN(HttpStatus.BAD_REQUEST, "Sistemde en az bir aktif sistem yöneticisi kalmalıdır."),
	USER_SUCCESSOR_REQUIRED(HttpStatus.BAD_REQUEST, "Bu personelin devredilmesi gereken sorumlulukları var: {0}. Lütfen devralacak personeli seçin."),
	USER_SUCCESSOR_INVALID(HttpStatus.BAD_REQUEST, "Devralacak personel, pasifleştirilen kişiden farklı, aktif ve bu yıla atanmış biri olmalıdır."),
	IMPORT_FILE_INVALID(HttpStatus.BAD_REQUEST, "Lütfen .csv uzantılı ve en fazla 2 MB boyutunda bir dosya seçin."),
	IMPORT_HEADER_INVALID(HttpStatus.BAD_REQUEST, "Dosyanın ilk satırı şablondaki başlıklarla aynı olmalıdır: ad;soyad;eposta;telefon;brans;sube;unvan"),
	IMPORT_EMPTY(HttpStatus.BAD_REQUEST, "Dosyada personel satırı bulunamadı."),
	IMPORT_TOO_MANY_ROWS(HttpStatus.BAD_REQUEST, "Bir seferde en fazla 1000 personel yüklenebilir."),
	IMPORT_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Dosyada düzeltilmesi gereken satırlar var. Hiçbir kayıt eklenmedi."),
	BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Şube bulunamadı."),
	BRANCH_NAME_TAKEN(HttpStatus.CONFLICT, "Bu adla kayıtlı bir şube var."),
	BRANCH_INACTIVE(HttpStatus.BAD_REQUEST, "Seçilen şube pasif durumda. Başka bir şube seçin."),
	BRANCH_IN_USE(HttpStatus.CONFLICT, "Bu şubeye atanmış {0} personel var. Önce bu personeli başka bir şubeye taşıyın."),
	BRANCH_LAST_ACTIVE(HttpStatus.BAD_REQUEST, "En az bir aktif şube bulunmalıdır."),
	SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "Eğitim-öğretim yılı bulunamadı."),
	SEASON_NAME_INVALID(HttpStatus.BAD_REQUEST, "Yıl adı 2026-2027 biçiminde olmalı ve ikinci yıl birinciden bir fazla olmalıdır."),
	SEASON_NAME_TAKEN(HttpStatus.CONFLICT, "Bu eğitim-öğretim yılı zaten var."),
	SEASON_NAME_NOT_NEWER(HttpStatus.BAD_REQUEST, "Yeni yıl, mevcut yıllardan daha sonraki bir yıl olmalıdır."),
	SEASON_DATES_INVALID(HttpStatus.BAD_REQUEST, "Bitiş tarihi başlangıç tarihinden sonra olmalıdır."),
	SEASON_DRAFT_EXISTS(HttpStatus.CONFLICT, "Hazırlık aşamasında bir yıl zaten var ({0}). Önce onu aktifleştirin veya silin."),
	SEASON_NOT_DRAFT(HttpStatus.CONFLICT, "Bu işlem yalnızca hazırlık aşamasındaki yıl için yapılabilir."),
	SEASON_CLOSED_READ_ONLY(HttpStatus.CONFLICT, "Kapanmış bir yılın bilgileri değiştirilemez."),
	SEASON_ACTIVATION_BLOCKED(HttpStatus.CONFLICT, "Yıl aktifleştirilemiyor."),
	SEASON_MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "Yıl ataması bulunamadı."),
	SEASON_MEMBER_EXISTS(HttpStatus.CONFLICT, "Bu personel bu yıla zaten atanmış."),
	SEASON_USER_PASSIVE(HttpStatus.BAD_REQUEST, "Pasif personel yıla atanamaz."),
	SEASON_APPROVER_NOT_MEMBER(HttpStatus.BAD_REQUEST, "Seçilen onaylayıcı bu yıla atanmış aktif bir personel olmalıdır."),
	SEASON_APPROVER_SELF(HttpStatus.BAD_REQUEST, "Personel kendi kendisinin onaylayıcısı olamaz."),
	SEASON_MEMBER_IS_APPROVER(HttpStatus.CONFLICT, "Bu kişi bu yıl {0} personelin fotokopi onaylayıcısı. Önce onların onaylayıcısını değiştirin."),
	SEASON_MEMBER_REMOVAL_BLOCKED(HttpStatus.CONFLICT, "Seçilen personel yıldan çıkarılamıyor."),
	SEASON_COPY_BRANCH_INACTIVE(HttpStatus.BAD_REQUEST, "Bazı personelin önceki yıldaki şubesi pasif. Önce şubeyi aktifleştirin veya bu kişileri seçimden çıkarın."),
	GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Grup bulunamadı."),
	GROUP_MANAGE_FORBIDDEN(HttpStatus.FORBIDDEN, "Grupları yalnızca yöneticiler düzenleyebilir."),
	GROUP_NAME_TAKEN(HttpStatus.CONFLICT, "Bu adla kayıtlı bir grup var."),
	GROUP_ARCHIVED(HttpStatus.CONFLICT, "Bu grup arşivlenmiş; değişiklik yapılamaz. Önce arşivden çıkarın."),
	GROUP_MEMBER_INVALID(HttpStatus.BAD_REQUEST, "Seçilen kişilerden bazıları aktif personel değil.");

	private final HttpStatus status;
	private final String message;

	CoreErrorCode(HttpStatus status, String message) {
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
