package tr.okul.iletisim.common.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldError> fieldErrors, List<String> details,
		String traceId) {

	public record FieldError(String field, String message) {
	}

}
