package tr.okul.iletisim.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import tr.okul.iletisim.common.error.BusinessException;
import tr.okul.iletisim.common.error.CommonErrorCode;
import tr.okul.iletisim.common.security.UserContext;
import tr.okul.iletisim.common.security.UserContextFilter;

public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return UserContext.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Object ctx = webRequest.getAttribute(UserContextFilter.ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		if (ctx instanceof UserContext userContext) {
			return userContext;
		}
		throw new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}

}
