package tr.okul.iletisim.core.directory;

public enum TargetType {

	USER("Kişi"), GROUP("Grup"), BRANCH("Şube"), ALL("Tüm Kurum");

	private final String label;

	TargetType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
