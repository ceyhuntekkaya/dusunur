package tr.okul.iletisim.core.user;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.core.spi.ImpactItem;
import tr.okul.iletisim.core.spi.UserDeactivationHook;

@Component
public class CoreUserDeactivationHook implements UserDeactivationHook {

	private final JdbcClient jdbc;

	public CoreUserDeactivationHook(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<ImpactItem> describe(long userId) {
		int n = jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN season s ON s.id = m.season_id
				WHERE m.print_approver_id = :uid AND s.status IN ('ACTIVE', 'DRAFT')
				""").param("uid", userId).query(Integer.class).single();
		return List.of(new ImpactItem("personelin fotokopi onaylayıcısı", n));
	}

	@Override
	public void transfer(long userId, Long successorUserId) {
		jdbc.sql("""
				UPDATE season_membership m
				SET print_approver_id = CASE
				      WHEN CAST(:succ AS bigint) IS NULL OR m.user_id = :succ
				        OR NOT EXISTS (SELECT 1 FROM season_membership x
				                        WHERE x.season_id = m.season_id AND x.user_id = :succ) THEN NULL
				      ELSE :succ END,
				    updated_at = now()
				WHERE m.print_approver_id = :uid
				  AND m.season_id IN (SELECT id FROM season WHERE status IN ('ACTIVE', 'DRAFT'))
				""").param("succ", successorUserId, java.sql.Types.BIGINT).param("uid", userId).update();
	}

}
