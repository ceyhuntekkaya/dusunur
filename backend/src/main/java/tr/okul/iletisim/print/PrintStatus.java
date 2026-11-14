package tr.okul.iletisim.print;

import java.util.List;

public enum PrintStatus {

	PENDING_APPROVAL("Onay Bekliyor"),
	APPROVED("Onaylandı"),
	REJECTED("Reddedildi"),
	IN_PROGRESS("Hazırlanıyor"),
	READY("Hazır"),
	DELIVERED("Teslim Edildi"),
	CANCELLED("İptal Edildi");

	public static final List<PrintStatus> OPEN = List.of(PENDING_APPROVAL, APPROVED, IN_PROGRESS, READY);

	private final String label;

	PrintStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
