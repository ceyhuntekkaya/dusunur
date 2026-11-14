package tr.okul.iletisim.print;

import java.util.List;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.core.spi.MembershipRemovalHook;

@Component
public class PrintMembershipRemovalHook implements MembershipRemovalHook {

	private final PrintQueries queries;

	public PrintMembershipRemovalHook(PrintQueries queries) {
		this.queries = queries;
	}

	@Override
	public List<String> blockers(long seasonId, long userId) {
		int n = queries.pendingApprovalCount(seasonId, userId);
		return n == 0 ? List.of() : List.of("Onayını bekleyen %d fotokopi talebi var.".formatted(n));
	}

}
