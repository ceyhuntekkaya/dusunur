package tr.okul.iletisim.core.spi;

import java.util.Map;

import tr.okul.iletisim.common.security.UserContext;

public interface BadgeProvider {

	Map<String, Integer> badges(UserContext me);

}
