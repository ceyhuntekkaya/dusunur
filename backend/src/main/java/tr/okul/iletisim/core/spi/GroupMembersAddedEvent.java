package tr.okul.iletisim.core.spi;

import java.util.List;

public record GroupMembersAddedEvent(long groupId, List<Long> userIds) {

}
