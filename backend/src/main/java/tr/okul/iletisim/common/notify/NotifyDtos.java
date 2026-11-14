package tr.okul.iletisim.common.notify;

import java.time.OffsetDateTime;

public final class NotifyDtos {

	private NotifyDtos() {
	}

	public record NotificationDto(long id, String type, String title, String body, String link,
			OffsetDateTime createdAt, boolean read) {

		public static NotificationDto from(Notification n) {
			return new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
					n.getCreatedAt(), n.getReadAt() != null);
		}

	}

	public record WsEnvelope(String type, Object payload) {
	}

	public record PushSubscriptionRequest(String endpoint, Keys keys) {

		public record Keys(String p256dh, String auth) {
		}

	}

	public record PushUnsubscribeRequest(String endpoint) {
	}

}
