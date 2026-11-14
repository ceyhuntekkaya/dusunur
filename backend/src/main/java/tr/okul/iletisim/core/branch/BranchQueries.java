package tr.okul.iletisim.core.branch;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.core.CoreErrorCode;
import tr.okul.iletisim.core.branch.BranchDtos.BranchRow;
import tr.okul.iletisim.core.branch.BranchDtos.DirBranch;

@Component
public class BranchQueries {

	private static final String ROWS = """
			SELECT b.id, b.name, b.active,
			       (SELECT count(*) FROM season_membership m JOIN season s ON s.id = m.season_id
			         WHERE m.branch_id = b.id AND s.status = 'ACTIVE')::int AS member_count
			FROM branch b
			""";

	private final JdbcClient jdbc;

	public BranchQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<DirBranch> activeBranches() {
		return jdbc.sql("SELECT id, name FROM branch WHERE active = true ORDER BY name COLLATE \"tr-x-icu\"")
				.query(DirBranch.class).list();
	}

	public List<BranchRow> rows() {
		return jdbc.sql(ROWS + " ORDER BY b.name COLLATE \"tr-x-icu\"").query(BranchRow.class).list();
	}

	public BranchRow row(long id) {
		return jdbc.sql(ROWS + " WHERE b.id = :id").param("id", id).query(BranchRow.class).optional()
				.orElseThrow(() -> new BusinessException(CoreErrorCode.BRANCH_NOT_FOUND));
	}

	public int blockingMemberCount(long branchId) {
		return jdbc.sql("""
				SELECT count(*) FROM season_membership m JOIN season s ON s.id = m.season_id
				WHERE m.branch_id = :id AND s.status IN ('ACTIVE', 'DRAFT')
				""").param("id", branchId).query(Integer.class).single();
	}

}
