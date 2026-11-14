package tr.okul.iletisim.print;

import java.util.List;

import org.springframework.stereotype.Component;

import tr.okul.iletisim.core.spi.ImpactItem;
import tr.okul.iletisim.core.spi.UserDeactivationHook;

@Component
public class PrintDeactivationHook implements UserDeactivationHook {

	private final PrintQueries queries;
	private final PrintRequestService service;
	private final PrintDelegationService delegationService;

	public PrintDeactivationHook(PrintQueries queries, PrintRequestService service,
			PrintDelegationService delegationService) {
		this.queries = queries;
		this.service = service;
		this.delegationService = delegationService;
	}

	@Override
	public List<ImpactItem> describe(long userId) {
		return List.of(new ImpactItem("onayını bekleyen fotokopi talebi", queries.pendingApprovalCount(userId)));
	}

	@Override
	public void transfer(long userId, Long successorUserId) {
		service.transferApprover(userId, successorUserId);
		delegationService.deleteCurrentFor(userId);
	}

}
