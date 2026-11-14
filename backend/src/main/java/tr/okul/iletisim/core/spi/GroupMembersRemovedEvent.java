package tr.okul.iletisim.core.spi;

import java.util.List;

public record GroupMembersRemovedEvent(long groupId, List<Long> userIds) {

}
