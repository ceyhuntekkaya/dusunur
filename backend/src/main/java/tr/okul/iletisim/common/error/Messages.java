package tr.okul.iletisim.common.error;

public final class Messages {

	private Messages() {
	}

	public static String format(String template, Object... args) {
		if (args == null || args.length == 0) {
			return template;
		}
		String result = template;
		for (int i = 0; i < args.length; i++) {
			result = result.replace("{" + i + "}", String.valueOf(args[i]));
		}
		return result;
	}

}
