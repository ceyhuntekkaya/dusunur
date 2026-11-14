package tr.okul.iletisim.common.security;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.error.ErrorCode;
import tr.okul.iletisim.common.error.ErrorResponseWriter;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.core.CoreErrorCode;

public class UserContextFilter extends OncePerRequestFilter {

	public static final String ATTRIBUTE = "userContext";

	private static final Set<String> KVKK_EXEMPT = Set.of("/api/auth/me", "/api/auth/kvkk", "/api/auth/kvkk/accept",
			"/api/auth/logout", "/api/auth/csrf");

	private static final Set<String> PUBLIC = Set.of("/api/auth/login", "/api/auth/logout", "/api/auth/csrf",
			"/api/auth/forgot-password", "/api/auth/password-token", "/api/health");

	private static final String Q1 = """
			SELECT u.id, u.first_name, u.last_name, u.status, u.system_admin, u.kvkk_version,
			       s.id AS season_id, s.name AS season_name,
			       m.id AS membership_id, m.branch_id, b.name AS branch_name, m.title,
			       m.is_manager, m.is_print_operator, m.can_create_dms_space, m.print_approver_id
			FROM app_user u
			LEFT JOIN season s ON s.status = 'ACTIVE'
			LEFT JOIN season_membership m ON m.season_id = s.id AND m.user_id = u.id
			LEFT JOIN branch b ON b.id = m.branch_id
			WHERE u.id = :uid
			""";

	private static final String Q2 = "SELECT group_id FROM staff_group_member WHERE user_id = :uid";

	record Row(Long id, String firstName, String lastName, String status, boolean systemAdmin, int kvkkVersion,
			Long seasonId, String seasonName, Long membershipId, Long branchId, String branchName, String title,
			Boolean isManager, Boolean isPrintOperator, Boolean canCreateDmsSpace, Long printApproverId) {
	}

	private final JdbcClient jdbc;
	private final AppProperties app;
	private final ErrorResponseWriter errors;

	public UserContextFilter(JdbcClient jdbc, AppProperties app, ErrorResponseWriter errors) {
		this.jdbc = jdbc;
		this.app = app;
		this.errors = errors;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		boolean covered = path.startsWith("/api/") || path.equals("/ws") || path.startsWith("/ws/");
		return !covered || PUBLIC.contains(path) || path.startsWith("/api/auth/password-token/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
			chain.doFilter(request, response);
			return;
		}
		long userId = Long.parseLong(auth.getName());
		Optional<Row> found = jdbc.sql(Q1).param("uid", userId).query(Row.class).optional();
		if (found.isEmpty() || !"ACTIVE".equals(found.get().status())) {
			reject(request, response, CoreErrorCode.AUTH_ACCOUNT_PASSIVE, true);
			return;
		}
		Row r = found.get();
		if (!r.systemAdmin()) {
			if (r.seasonId() == null) {
				reject(request, response, CoreErrorCode.AUTH_NO_ACTIVE_SEASON, true);
				return;
			}
			if (r.membershipId() == null) {
				reject(request, response, CoreErrorCode.AUTH_NO_SEASON_MEMBERSHIP, true, r.seasonName());
				return;
			}
		}
		if (r.kvkkVersion() < app.kvkk().version() && !KVKK_EXEMPT.contains(request.getRequestURI())) {
			reject(request, response, CommonErrorCode.KVKK_REQUIRED, false);
			return;
		}
		List<Long> groups = jdbc.sql(Q2).param("uid", userId).query(Long.class).list();
		UserContext.Membership membership = r.membershipId() == null ? null
				: new UserContext.Membership(r.membershipId(), r.branchId(), r.branchName(), r.title(),
						Boolean.TRUE.equals(r.isManager()), Boolean.TRUE.equals(r.isPrintOperator()),
						Boolean.TRUE.equals(r.canCreateDmsSpace()), r.printApproverId());
		request.setAttribute(ATTRIBUTE, new UserContext(r.id(), r.firstName(), r.lastName(), r.systemAdmin(),
				r.seasonId(), r.seasonName(), membership, new HashSet<>(groups)));
		chain.doFilter(request, response);
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, ErrorCode code,
			boolean invalidateSession, Object... args) throws IOException {
		if (invalidateSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			SecurityContextHolder.clearContext();
		}
		errors.write(response, code, args);
	}

}
