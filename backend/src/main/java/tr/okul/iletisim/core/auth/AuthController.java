package tr.okul.iletisim.core.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.core.auth.AuthDtos.ChangePasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.ForgotPasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.KvkkResponse;
import tr.okul.iletisim.core.auth.AuthDtos.LoginRequest;
import tr.okul.iletisim.core.auth.AuthDtos.MeResponse;
import tr.okul.iletisim.core.auth.AuthDtos.MessageResponse;
import tr.okul.iletisim.core.auth.AuthDtos.SetPasswordRequest;
import tr.okul.iletisim.core.auth.AuthDtos.TokenInfoResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService service;
	private final MeQueries meQueries;

	public AuthController(AuthService service, MeQueries meQueries) {
		this.service = service;
		this.meQueries = meQueries;
	}

	@GetMapping("/csrf")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void csrf() {
	}

	@PostMapping("/login")
	public MeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		return service.login(request, httpRequest, httpResponse);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		service.logout(request);
	}

	@GetMapping("/me")
	public MeResponse me(UserContext me) {
		return meQueries.me(me.userId());
	}

	@GetMapping("/kvkk")
	public KvkkResponse kvkk() {
		return service.kvkk();
	}

	@PostMapping("/kvkk/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptKvkk(UserContext me) {
		service.acceptKvkk(me);
	}

	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(UserContext me, @Valid @RequestBody ChangePasswordRequest request,
			HttpServletRequest httpRequest) {
		service.changePassword(me, request, httpRequest.getSession().getId());
	}

	@PostMapping("/forgot-password")
	public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		return service.forgotPassword(request);
	}

	@GetMapping("/password-token/{token}")
	public TokenInfoResponse tokenInfo(@PathVariable String token) {
		return service.tokenInfo(token);
	}

	@PostMapping("/password-token")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void setPassword(@Valid @RequestBody SetPasswordRequest request) {
		service.setPassword(request);
	}

}
