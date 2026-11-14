package tr.okul.iletisim.feed;

import tr.okul.iletisim.common.audit.AuditAction;

public enum FeedAuditAction implements AuditAction {

	FEED_POST_DELETED("Akış gönderisi silindi"),
	FEED_POST_PINNED("Akış gönderisi sabitlendi"),
	FEED_POST_UNPINNED("Akış sabitlemesi kaldırıldı"),
	FEED_COMMENT_DELETED("Akış yorumu silindi");

	private final String label;

	FeedAuditAction(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}

}
