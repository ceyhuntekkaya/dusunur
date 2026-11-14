package tr.okul.iletisim.common.web;

import java.util.List;

public record CursorResponse<T>(List<T> items, Long nextCursor) {

}
