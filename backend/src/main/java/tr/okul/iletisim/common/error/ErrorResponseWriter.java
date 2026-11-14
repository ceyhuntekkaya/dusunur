package tr.okul.iletisim.common.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ErrorResponseWriter {

	public static final String TRACE_ID_KEY = "traceId";

	private final JsonMapper jsonMapper;

	public ErrorResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(HttpServletResponse response, ErrorCode code, Object... args) throws IOException {
		write(response, code.status(), new ApiError(code.name(), Messages.format(code.message(), args), List.of(),
				List.of(), MDC.get(TRACE_ID_KEY)));
	}

	public void write(HttpServletResponse response, HttpStatus status, ApiError body) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
