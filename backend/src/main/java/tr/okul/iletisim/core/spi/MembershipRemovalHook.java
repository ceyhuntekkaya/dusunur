package tr.okul.iletisim.core.spi;

import java.util.List;

public interface MembershipRemovalHook {

	List<String> blockers(long seasonId, long userId);

}
