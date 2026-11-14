package tr.okul.iletisim.dms;

public enum DmsLevel {

	VIEW(1, "Görüntüleme"), EDIT(2, "Düzenleme"), MANAGE(3, "Yönetim");

	private final int rank;
	private final String label;

	DmsLevel(int rank, String label) {
		this.rank = rank;
		this.label = label;
	}

	public int rank() {
		return rank;
	}

	public String label() {
		return label;
	}

	public boolean atLeast(DmsLevel other) {
		return rank >= other.rank;
	}

	public static DmsLevel ofRank(int rank) {
		for (DmsLevel l : values()) {
			if (l.rank == rank) {
				return l;
			}
		}
		throw new IllegalArgumentException("Geçersiz seviye: " + rank);
	}

}
