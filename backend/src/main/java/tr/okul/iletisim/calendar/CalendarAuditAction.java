package tr.okul.iletisim.calendar;

import tr.okul.iletisim.common.audit.AuditAction;

public enum CalendarAuditAction implements AuditAction {

	CAL_RESOURCE_CREATED("Salon/laboratuvar tanımlandı"),
	CAL_RESOURCE_UPDATED("Salon/laboratuvar güncellendi"),
	CAL_RESOURCE_DELETED("Salon/laboratuvar silindi"),
	CAL_ENTRY_DELETED("Başkasına ait takvim kaydı silindi");

	private final String label;

	CalendarAuditAction(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

}
