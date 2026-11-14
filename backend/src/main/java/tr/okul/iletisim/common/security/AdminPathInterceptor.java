package tr.okul.iletisim.common.security;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.core.CoreErrorCode;

public class AdminPathInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (request.getAttribute(UserContextFilter.ATTRIBUTE) instanceof UserContext ctx && ctx.systemAdmin()) {
			return true;
		}
		throw new BusinessException(CoreErrorCode.CORE_ADMIN_ONLY);
	}

}
