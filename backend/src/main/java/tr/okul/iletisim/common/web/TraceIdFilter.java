package tr.okul.iletisim.common.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tr.okul.iletisim.common.error.ErrorResponseWriter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		MDC.put(ErrorResponseWriter.TRACE_ID_KEY, traceId);
		response.setHeader("X-Trace-Id", traceId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			MDC.remove(ErrorResponseWriter.TRACE_ID_KEY);
		}
	}

}
