package tr.okul.iletisim.calendar;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.util.TextNormalizer;
import tr.okul.iletisim.core.spi.GroupArchivedEvent;
import tr.okul.iletisim.core.spi.GroupCreatedEvent;
import tr.okul.iletisim.core.spi.GroupUpdatedEvent;
import tr.okul.iletisim.core.spi.UserDeactivatedEvent;

@Component
@Order(110)
public class CalendarBootstrap implements ApplicationRunner {

	public static final String INSTITUTION_NAME = "Kurum Takvimi";
	public static final String DEFAULT_NAME = "Kişisel Takvimim";

	private final JdbcClient jdbc;

	public CalendarBootstrap(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		ensureSystem();
	}

	@Transactional
	public void ensure(UserContext me) {
		ensureSystem();
		jdbc.sql("""
				INSERT INTO cal_calendar (type, name, name_key, owner_id, is_default, created_by)
				SELECT 'PERSONAL', :name, :key, :uid, true, :uid
				WHERE NOT EXISTS (SELECT 1 FROM cal_calendar WHERE owner_id = :uid AND is_default)
				ON CONFLICT DO NOTHING
				""").param("name", DEFAULT_NAME).param("key", TextNormalizer.fold(DEFAULT_NAME))
				.param("uid", me.userId()).update();
	}

	@Transactional
	public void ensureSystem() {
		jdbc.sql("""
				INSERT INTO cal_calendar (type, name, name_key)
				SELECT 'INSTITUTION', :name, :key
				WHERE NOT EXISTS (SELECT 1 FROM cal_calendar WHERE type = 'INSTITUTION')
				ON CONFLICT DO NOTHING
				""").param("name", INSTITUTION_NAME).param("key", TextNormalizer.fold(INSTITUTION_NAME)).update();
		jdbc.sql("""
				INSERT INTO cal_calendar (type, name, name_key, branch_id)
				SELECT 'BRANCH', b.name, b.name_key, b.id FROM branch b
				WHERE NOT EXISTS (SELECT 1 FROM cal_calendar c WHERE c.type = 'BRANCH' AND c.branch_id = b.id)
				ON CONFLICT DO NOTHING
				""").update();
		jdbc.sql("""
				INSERT INTO cal_calendar (type, name, name_key, staff_group_id, read_only)
				SELECT 'GROUP', g.name, g.name_key, g.id, g.archived FROM staff_group g
				WHERE NOT EXISTS (SELECT 1 FROM cal_calendar c WHERE c.type = 'GROUP' AND c.staff_group_id = g.id)
				ON CONFLICT DO NOTHING
				""").update();
	}

	@EventListener
	public void onGroupCreated(GroupCreatedEvent e) {
		jdbc.sql("""
				INSERT INTO cal_calendar (type, name, name_key, staff_group_id)
				VALUES ('GROUP', :name, :key, :gid)
				ON CONFLICT DO NOTHING
				""").param("name", e.name()).param("key", TextNormalizer.fold(e.name())).param("gid", e.groupId())
				.update();
	}

	@EventListener
	public void onGroupUpdated(GroupUpdatedEvent e) {
		jdbc.sql("""
				UPDATE cal_calendar SET name = :name, name_key = :key, updated_at = now()
				WHERE type = 'GROUP' AND staff_group_id = :gid
				""").param("name", e.name()).param("key", TextNormalizer.fold(e.name())).param("gid", e.groupId())
				.update();
	}

	@EventListener
	public void onGroupArchived(GroupArchivedEvent e) {
		jdbc.sql("UPDATE cal_calendar SET read_only = :r, updated_at = now() WHERE type = 'GROUP' AND staff_group_id = :gid")
				.param("r", e.archived()).param("gid", e.groupId()).update();
	}

	@EventListener
	public void onUserDeactivated(UserDeactivatedEvent e) {
		jdbc.sql("DELETE FROM cal_share WHERE subject_type = 'USER' AND subject_id = :uid").param("uid", e.userId())
				.update();
	}

}
