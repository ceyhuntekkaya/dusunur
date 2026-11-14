package tr.okul.iletisim.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

	private static final Locale TR = Locale.forLanguageTag("tr");
	private static final int MAX_QUERY = 100;

	private TextNormalizer() {
	}

	public static String clean(String s) {
		return s == null ? null : s.trim().replaceAll("\\s+", " ");
	}

	public static String fold(String s) {
		if (s == null) {
			return "";
		}
		String r = s.toLowerCase(TR)
				.replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u').replace('ş', 's')
				.replace('ö', 'o').replace('ç', 'c').replace('â', 'a').replace('î', 'i').replace('û', 'u');
		r = Normalizer.normalize(r, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return r.trim().replaceAll("\\s+", " ");
	}

	public static String lowerEmail(String s) {
		return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
	}

	public static String truncateQuery(String q) {
		if (q == null) {
			return "";
		}
		String t = q.trim();
		return t.length() > MAX_QUERY ? t.substring(0, MAX_QUERY) : t;
	}

	public static String likePattern(String q) {
		String f = fold(truncateQuery(q));
		String escaped = f.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
