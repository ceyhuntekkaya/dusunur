package tr.okul.iletisim.print;

import java.util.Map;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.spi.BadgeProvider;

@Component
public class PrintBadgeProvider implements BadgeProvider {

	private final PrintQueries queries;

	public PrintBadgeProvider(PrintQueries queries) {
		this.queries = queries;
	}

	@Override
	public Map<String, Integer> badges(UserContext me) {
		if (!me.isMember() || me.seasonId() == null) {
			return Map.of("printApprovals", 0, "printQueue", 0);
		}
		int approvals = queries.pendingApprovalCount(me.seasonId(), me.userId());
		int queue = me.membership().printOperator() ? queries.queueCount(me.seasonId(), me.membership().branchId()) : 0;
		return Map.of("printApprovals", approvals, "printQueue", queue);
	}

}
