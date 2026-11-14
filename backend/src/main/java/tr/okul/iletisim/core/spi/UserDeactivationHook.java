package tr.okul.iletisim.core.spi;

import java.util.List;

public interface UserDeactivationHook {

	List<ImpactItem> describe(long userId);

	void transfer(long userId, Long successorUserId);

}
