package tr.okul.iletisim.chat;

import java.util.List;

final class ChatText {

	private ChatText() {
	}

	static String snippet(String content, List<String> kinds) {
		String s = content == null ? "" : content.replaceAll("\\s+", " ").trim();
		if (!s.isEmpty()) {
			return s.length() > 80 ? s.substring(0, 80) : s;
		}
		if (kinds.isEmpty()) {
			return "";
		}
		String base = switch (kinds.get(0)) {
			case "IMAGE" -> "📷 Fotoğraf";
			case "VIDEO" -> "🎥 Video";
			default -> "📎 Dosya";
		};
		return kinds.size() > 1 ? base + " (+" + (kinds.size() - 1) + ")" : base;
	}

	static String kind(String contentType) {
		if (contentType.startsWith("image/")) {
			return "IMAGE";
		}
		return contentType.startsWith("video/") ? "VIDEO" : "FILE";
	}

}
