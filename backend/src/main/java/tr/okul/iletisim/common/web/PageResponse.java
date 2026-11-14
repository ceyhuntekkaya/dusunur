package tr.okul.iletisim.common.web;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long total) {

	public static final int MAX_SIZE = 100;

	public static int clampSize(int size) {
		return Math.max(1, Math.min(size, MAX_SIZE));
	}

}
