package tr.okul.iletisim.print;

import tr.okul.iletisim.common.audit.AuditAction;

public enum PrintAuditAction implements AuditAction {

	PRINT_SECRET_FILE_ACCESSED("Gizli fotokopi dosyası açıldı"),
	PRINT_DELEGATION_CREATED("Fotokopi vekili tanımlandı"),
	PRINT_DELEGATION_DELETED("Fotokopi vekâleti silindi");

	private final String label;

	PrintAuditAction(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

}
