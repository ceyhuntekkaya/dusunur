package tr.okul.iletisim.dms;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.util.TextNormalizer;

@Component
public class DmsNameRules {

	public static final int FOLDER_MAX = 100;
	public static final int FILE_MAX = 200;

	private static final Locale TR = Locale.forLanguageTag("tr");
	private static final Pattern INVALID = Pattern.compile("[/\\\\:*?\"<>|\\p{Cntrl}]");

	public String folderName(String raw) {
		return validate(raw, FOLDER_MAX);
	}

	public String fileName(String raw) {
		return validate(raw, FILE_MAX);
	}

	public String sanitize(String raw) {
		String n = raw == null ? "" : INVALID.matcher(raw).replaceAll("_").trim();
		if (n.isEmpty() || n.equals(".") || n.equals("..")) {
			return "dosya";
		}
		if (n.length() > FILE_MAX) {
			String ext = extension(n);
			if (ext.length() > 20) {
				ext = "";
			}
			n = n.substring(0, FILE_MAX - ext.length()) + ext;
		}
		return n;
	}

	public String key(String name) {
		return name.toLowerCase(TR);
	}

	public String searchKey(String name) {
		return TextNormalizer.fold(name);
	}

	public String extension(String name) {
		int dot = name.lastIndexOf('.');
		return dot <= 0 ? "" : name.substring(dot);
	}

	public String uniqueName(String name, int maxLen, Predicate<String> taken) {
		if (!taken.test(name)) {
			return name;
		}
		String ext = extension(name);
		String base = name.substring(0, name.length() - ext.length());
		for (int n = 2;; n++) {
			String suffix = " (" + n + ")" + ext;
			String cut = base.length() + suffix.length() > maxLen ? base.substring(0, maxLen - suffix.length()) : base;
			String candidate = cut + suffix;
			if (!taken.test(candidate)) {
				return candidate;
			}
		}
	}

	private String validate(String raw, int max) {
		String n = raw == null ? "" : raw.trim();
		if (n.isEmpty() || n.length() > max || n.equals(".") || n.equals("..") || INVALID.matcher(n).find()) {
			throw new BusinessException(DmsErrorCode.DMS_NAME_INVALID);
		}
		return n;
	}

}
