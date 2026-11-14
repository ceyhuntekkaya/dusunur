package tr.okul.iletisim.calendar;

public enum CalShareLevel {

	VIEW(1), EDIT(2);

	private final int rank;

	CalShareLevel(int rank) {
		this.rank = rank;
	}

	public int rank() {
		return rank;
	}

	public static CalShareLevel ofRank(int rank) {
		for (CalShareLevel l : values()) {
			if (l.rank == rank) {
				return l;
			}
		}
		throw new IllegalArgumentException("Geçersiz seviye: " + rank);
	}

}
