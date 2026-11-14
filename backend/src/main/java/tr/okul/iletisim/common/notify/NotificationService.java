package tr.okul.iletisim.common.notify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.notify.NotifyDtos.NotificationDto;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.web.CursorResponse;
import tr.okul.iletisim.common.web.PageResponse;

@Service
public class NotificationService {

	private final NotificationRepository repository;
	private final RealtimePublisher realtime;
	private final WebPushSender pushSender;
	private final JdbcClient jdbc;

	public NotificationService(NotificationRepository repository, RealtimePublisher realtime,
			WebPushSender pushSender, JdbcClient jdbc) {
		this.repository = repository;
		this.realtime = realtime;
		this.pushSender = pushSender;
		this.jdbc = jdbc;
	}

	@Transactional
	public void notify(Collection<Long> userIds, String type, String title, String body, String link) {
		List<Long> ids = distinct(userIds);
		if (ids.isEmpty()) {
			return;
		}
		List<Notification> rows = new ArrayList<>();
		for (Long userId : ids) {
			Notification n = new Notification();
			n.setUserId(userId);
			n.setType(cut(type, 50));
			n.setTitle(cut(title, 200));
			n.setBody(cut(body, 500));
			n.setLink(cut(link, 300));
			rows.add(n);
		}
		List<Notification> saved = repository.saveAllAndFlush(rows);
		for (Notification n : saved) {
			realtime.toUsers(List.of(n.getUserId()), "NOTIFICATION", NotificationDto.from(n));
		}
		RealtimePublisher.afterCommit(() -> pushSender.send(ids, title, body, link, null));
	}

	public void pushOnly(Collection<Long> userIds, String title, String body, String link, String tag) {
		List<Long> ids = distinct(userIds);
		if (ids.isEmpty()) {
			return;
		}
		RealtimePublisher.afterCommit(() -> pushSender.send(ids, title, body, link, tag));
	}

	@Transactional(readOnly = true)
	public CursorResponse<NotificationDto> list(UserContext me, Long before, int size) {
		int limit = Math.max(1, Math.min(size, PageResponse.MAX_SIZE));
		List<NotificationDto> rows = jdbc.sql("""
				SELECT id, type, title, body, link, created_at, (read_at IS NOT NULL) AS read
				FROM notification
				WHERE user_id = :uid AND (CAST(:before AS bigint) IS NULL OR id < CAST(:before AS bigint))
				ORDER BY id DESC LIMIT :limit
				""").param("uid", me.userId()).param("before", before).param("limit", limit + 1)
				.query(NotificationDto.class).list();
		if (rows.size() > limit) {
			List<NotificationDto> items = rows.subList(0, limit);
			return new CursorResponse<>(List.copyOf(items), items.get(limit - 1).id());
		}
		return new CursorResponse<>(rows, null);
	}

	@Transactional
	public void markRead(UserContext me, long id) {
		int updated = jdbc.sql("""
				UPDATE notification SET read_at = COALESCE(read_at, now()) WHERE id = :id AND user_id = :uid
				""").param("id", id).param("uid", me.userId()).update();
		if (updated == 0) {
			throw new BusinessException(CommonErrorCode.NOT_FOUND);
		}
	}

	@Transactional
	public void markAllRead(UserContext me) {
		jdbc.sql("UPDATE notification SET read_at = now() WHERE user_id = :uid AND read_at IS NULL")
				.param("uid", me.userId()).update();
	}

	private static List<Long> distinct(Collection<Long> userIds) {
		return new ArrayList<>(new LinkedHashSet<>(userIds));
	}

	private static String cut(String s, int max) {
		return s == null || s.length() <= max ? s : s.substring(0, max);
	}

}
