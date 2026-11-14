package tr.okul.iletisim.common.security;

import java.util.Set;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SessionService {

	private final FindByIndexNameSessionRepository<?> sessions;

	public SessionService(FindByIndexNameSessionRepository<?> sessions) {
		this.sessions = sessions;
	}

	public void invalidateAll(long userId, String exceptSessionId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					doInvalidate(userId, exceptSessionId);
				}
			});
		}
		else {
			doInvalidate(userId, exceptSessionId);
		}
	}

	private void doInvalidate(long userId, String exceptSessionId) {
		Set<String> ids = sessions.findByPrincipalName(String.valueOf(userId)).keySet();
		for (String id : ids) {
			if (!id.equals(exceptSessionId)) {
				sessions.deleteById(id);
			}
		}
	}

}
