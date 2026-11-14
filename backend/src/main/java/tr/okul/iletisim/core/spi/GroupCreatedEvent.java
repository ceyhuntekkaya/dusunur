package tr.okul.iletisim.core.spi;

import java.util.List;

public record GroupCreatedEvent(long groupId, String name, List<Long> memberIds) {

}
