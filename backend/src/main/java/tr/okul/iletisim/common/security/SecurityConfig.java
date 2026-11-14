package tr.okul.iletisim.common.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.error.ErrorResponseWriter;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorResponseWriter errors, JdbcClient jdbc,
			AppProperties app, SecurityContextRepository contextRepository) throws Exception {
		http
				.csrf(c -> c.spa())
				.formLogin(f -> f.disable())
				.httpBasic(b -> b.disable())
				.logout(l -> l.disable())
				.requestCache(r -> r.disable())
				.securityContext(c -> c.securityContextRepository(contextRepository))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/login", "/api/auth/logout", "/api/auth/csrf",
								"/api/auth/forgot-password", "/api/auth/password-token",
								"/api/auth/password-token/**", "/api/health")
						.permitAll()
						.requestMatchers("/api/**", "/ws/**", "/ws").authenticated()
						.anyRequest().authenticated())
				.exceptionHandling(e -> e
						.authenticationEntryPoint((request, response, ex) -> errors.write(response,
								CommonErrorCode.UNAUTHENTICATED))
						.accessDeniedHandler((request, response, ex) -> errors.write(response,
								ex instanceof CsrfException ? CommonErrorCode.CSRF_INVALID
										: CommonErrorCode.FORBIDDEN)))
				.addFilterAfter(new CsrfCookieFilter(), AuthorizationFilter.class)
				.addFilterAfter(new UserContextFilter(jdbc, app, errors), CsrfCookieFilter.class);
		return http.build();
	}

	static class CsrfCookieFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (token != null) {
				token.getToken();
			}
			chain.doFilter(request, response);
		}

	}

}
