package tr.okul.iletisim.feed;

final class FeedText {

	private FeedText() {
	}

	static String snippet(String text, int max) {
		String s = text == null ? "" : text.replaceAll("\\s+", " ").trim();
		return s.length() > max ? s.substring(0, max) : s;
	}

	static String trimToNull(String text) {
		if (text == null) {
			return null;
		}
		String s = text.trim();
		return s.isEmpty() ? null : s;
	}

}
