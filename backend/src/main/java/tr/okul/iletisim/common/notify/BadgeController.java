package tr.okul.iletisim.common.notify;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.spi.BadgeProvider;

@RestController
public class BadgeController {

	private final List<BadgeProvider> providers;

	public BadgeController(List<BadgeProvider> providers) {
		this.providers = providers;
	}

	@GetMapping("/api/me/badges")
	public Map<String, Integer> badges(UserContext me) {
		Map<String, Integer> result = new LinkedHashMap<>();
		for (BadgeProvider p : providers) {
			result.putAll(p.badges(me));
		}
		return result;
	}

}
