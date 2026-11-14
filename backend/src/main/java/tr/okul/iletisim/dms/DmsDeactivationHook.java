package tr.okul.iletisim.dms;

import java.util.List;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.core.spi.ImpactItem;
import tr.okul.iletisim.core.spi.UserDeactivationHook;

@Component
public class DmsDeactivationHook implements UserDeactivationHook {

	private final DmsSpaceService spaces;

	public DmsDeactivationHook(DmsSpaceService spaces) {
		this.spaces = spaces;
	}

	@Override
	public List<ImpactItem> describe(long userId) {
		return List.of(new ImpactItem("doküman alanının sahibi", spaces.ownedCount(userId)));
	}

	@Override
	public void transfer(long userId, Long successorUserId) {
		spaces.transferOwnership(userId, successorUserId);
	}

}
