package tr.okul.iletisim.common.notify;

import java.util.Collection;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tr.okul.iletisim.common.notify.NotifyDtos.WsEnvelope;

@Component
public class RealtimePublisher {

	private final SimpMessagingTemplate template;

	public RealtimePublisher(SimpMessagingTemplate template) {
		this.template = template;
	}

	public void toUsers(Collection<Long> userIds, String type, Object payload) {
		WsEnvelope envelope = new WsEnvelope(type, payload);
		afterCommit(() -> {
			for (Long userId : userIds) {
				template.convertAndSendToUser(String.valueOf(userId), "/queue/events", envelope);
			}
		});
	}

	public static void afterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		}
		else {
			action.run();
		}
	}

}
