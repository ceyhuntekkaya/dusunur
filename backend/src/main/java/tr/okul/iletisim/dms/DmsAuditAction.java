package tr.okul.iletisim.dms;

import tr.okul.iletisim.common.audit.AuditAction;

public enum DmsAuditAction implements AuditAction {

	DMS_SPACE_CREATED("Doküman alanı oluşturuldu"),
	DMS_SPACE_UPDATED("Doküman alanı güncellendi"),
	DMS_SPACE_DELETED("Doküman alanı silindi"),
	DMS_GRANT_CHANGED("Doküman alanı yetkisi değiştirildi"),
	DMS_ITEM_TRASHED("Doküman çöp kutusuna taşındı"),
	DMS_ITEM_RESTORED("Doküman geri yüklendi"),
	DMS_ITEM_PURGED("Doküman kalıcı olarak silindi"),
	DMS_TRASH_EMPTIED("Çöp kutusu boşaltıldı");

	private final String label;

	DmsAuditAction(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

}
