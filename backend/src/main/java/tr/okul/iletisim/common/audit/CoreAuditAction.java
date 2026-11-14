package tr.okul.iletisim.common.audit;

public enum CoreAuditAction implements AuditAction {

	AUTH_LOGIN_SUCCESS("Giriş yapıldı"),
	AUTH_LOGIN_FAILED("Hatalı giriş denemesi"),
	AUTH_ACCOUNT_LOCKED("Hesap geçici olarak kilitlendi"),
	AUTH_PASSWORD_CHANGED("Şifre değiştirildi"),
	AUTH_PASSWORD_SET("Şifre belirlendi"),
	KVKK_ACCEPTED("Aydınlatma metni onaylandı"),
	USER_CREATED("Personel eklendi"),
	USER_UPDATED("Personel güncellendi"),
	USER_IMPORTED("Toplu personel yüklendi"),
	USER_SETUP_LINK_SENT("Şifre bağlantısı gönderildi"),
	USER_DEACTIVATED("Personel pasifleştirildi"),
	USER_REACTIVATED("Personel aktifleştirildi"),
	BRANCH_CREATED("Şube eklendi"),
	BRANCH_UPDATED("Şube güncellendi"),
	BRANCH_DEACTIVATED("Şube pasifleştirildi"),
	BRANCH_ACTIVATED("Şube aktifleştirildi"),
	SEASON_CREATED("Eğitim-öğretim yılı oluşturuldu"),
	SEASON_UPDATED("Eğitim-öğretim yılı güncellendi"),
	SEASON_DELETED("Eğitim-öğretim yılı silindi"),
	SEASON_ACTIVATED("Eğitim-öğretim yılı aktifleştirildi"),
	SEASON_MEMBERSHIP_ADDED("Yıla personel atandı"),
	SEASON_MEMBERSHIP_UPDATED("Yıl ataması güncellendi"),
	SEASON_MEMBERSHIP_REMOVED("Personel yıldan çıkarıldı"),
	SEASON_MEMBERSHIPS_COPIED("Önceki yıldan atama kopyalandı"),
	GROUP_CREATED("Grup oluşturuldu"),
	GROUP_UPDATED("Grup güncellendi"),
	GROUP_MEMBERS_ADDED("Gruba üye eklendi"),
	GROUP_MEMBER_REMOVED("Gruptan üye çıkarıldı"),
	GROUP_ARCHIVED("Grup arşivlendi"),
	GROUP_UNARCHIVED("Grup arşivden çıkarıldı");

	private final String label;

	CoreAuditAction(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

}
