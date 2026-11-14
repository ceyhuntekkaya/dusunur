package tr.okul.iletisim.core.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.core.auth.AuthDtos.MeResponse;

@Component
public class MeQueries {

	record Row(long id, String firstName, String lastName, String email, String phone, String discipline,
			boolean systemAdmin, int kvkkVersion, Long seasonId, String seasonName, Long membershipId, Long branchId,
			String branchName, String title, Boolean isManager, Boolean isPrintOperator, Boolean canCreateDmsSpace,
			Long printApproverId, String approverName) {
	}

	private final JdbcClient jdbc;
	private final AppProperties app;

	public MeQueries(JdbcClient jdbc, AppProperties app) {
		this.jdbc = jdbc;
		this.app = app;
	}

	public MeResponse me(long userId) {
		Row r = jdbc.sql("""
				SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.discipline, u.system_admin, u.kvkk_version,
				       s.id AS season_id, s.name AS season_name,
				       m.id AS membership_id, m.branch_id, b.name AS branch_name, m.title,
				       m.is_manager, m.is_print_operator, m.can_create_dms_space, m.print_approver_id,
				       ap.first_name || ' ' || ap.last_name AS approver_name
				FROM app_user u
				LEFT JOIN season s ON s.status = 'ACTIVE'
				LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id
				LEFT JOIN branch b ON b.id = m.branch_id
				LEFT JOIN app_user ap ON ap.id = m.print_approver_id
				WHERE u.id = :uid
				""").param("uid", userId).query(Row.class).optional()
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED));

		boolean member = r.membershipId() != null;
		boolean manager = member && Boolean.TRUE.equals(r.isManager());
		boolean printOperator = member && Boolean.TRUE.equals(r.isPrintOperator());
		boolean dms = manager || (member && Boolean.TRUE.equals(r.canCreateDmsSpace()));
		MeResponse.Membership membership = member
				? new MeResponse.Membership(r.branchId(), r.branchName(), r.title(), manager, printOperator,
						Boolean.TRUE.equals(r.canCreateDmsSpace()), r.printApproverId(), r.approverName())
				: null;
		MeResponse.Season season = r.seasonId() == null ? null : new MeResponse.Season(r.seasonId(), r.seasonName());
		return new MeResponse(r.id(), r.firstName(), r.lastName(), r.firstName() + " " + r.lastName(), r.email(),
				r.phone(), r.discipline(), r.systemAdmin(), r.kvkkVersion() < app.kvkk().version(), season, membership,
				new MeResponse.Permissions(member, manager, manager, manager || r.systemAdmin(), manager, dms,
						printOperator, r.systemAdmin()));
	}

}
