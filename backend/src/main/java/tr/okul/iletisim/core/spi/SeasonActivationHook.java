package tr.okul.iletisim.core.spi;

import java.util.List;

public interface SeasonActivationHook {

	List<String> preview(long closingSeasonId);

	void onActivated(long closedSeasonId, long newSeasonId);

}
