package tr.okul.iletisim.common.util;

import java.util.ArrayList;
import java.util.List;

public final class Csv {

	private Csv() {
	}

	public static char detectDelimiter(String content) {
		int nl = content.indexOf('\n');
		String first = nl < 0 ? content : content.substring(0, nl);
		return first.indexOf(';') >= 0 ? ';' : ',';
	}

	public static List<List<String>> parse(String content, char delimiter) {
		List<List<String>> rows = new ArrayList<>();
		List<String> row = new ArrayList<>();
		StringBuilder cell = new StringBuilder();
		boolean quoted = false;
		boolean cellStarted = false;
		int n = content.length();
		for (int i = 0; i < n; i++) {
			char c = content.charAt(i);
			if (quoted) {
				if (c == '"') {
					if (i + 1 < n && content.charAt(i + 1) == '"') {
						cell.append('"');
						i++;
					}
					else {
						quoted = false;
					}
				}
				else {
					cell.append(c);
				}
			}
			else if (c == '"' && cell.isEmpty()) {
				quoted = true;
				cellStarted = true;
			}
			else if (c == delimiter) {
				row.add(cell.toString());
				cell.setLength(0);
				cellStarted = true;
			}
			else if (c == '\r' || c == '\n') {
				if (c == '\r' && i + 1 < n && content.charAt(i + 1) == '\n') {
					i++;
				}
				row.add(cell.toString());
				cell.setLength(0);
				rows.add(row);
				row = new ArrayList<>();
				cellStarted = false;
			}
			else {
				cell.append(c);
				cellStarted = true;
			}
		}
		if (cellStarted || !cell.isEmpty() || !row.isEmpty()) {
			row.add(cell.toString());
			rows.add(row);
		}
		return rows;
	}

	public static String escape(String value, char delimiter) {
		if (value == null) {
			return "";
		}
		if (value.indexOf(delimiter) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
				|| value.indexOf('\r') >= 0) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

}
