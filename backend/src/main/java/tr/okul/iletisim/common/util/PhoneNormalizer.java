package tr.okul.iletisim.common.util;

import java.util.regex.Pattern;

public final class PhoneNormalizer {

	private static final Pattern VALID = Pattern.compile("^(?:90|0)?5\\d{9}$");

	private PhoneNormalizer() {
	}

	public static boolean isValid(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		return VALID.matcher(raw.replaceAll("\\D", "")).matches();
	}

	/** Boş girdide null döner; geçersiz girdide IllegalArgumentException fırlatır. */
	public static String normalize(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String digits = raw.replaceAll("\\D", "");
		if (!VALID.matcher(digits).matches()) {
			throw new IllegalArgumentException("Geçersiz telefon numarası");
		}
		return "+90" + digits.substring(digits.length() - 10);
	}

}
