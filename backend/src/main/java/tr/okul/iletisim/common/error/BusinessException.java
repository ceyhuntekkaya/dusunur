package tr.okul.iletisim.common.error;

import java.util.List;

public class BusinessException extends RuntimeException {

	private final ErrorCode code;
	private final transient List<ApiError.FieldError> fieldErrors;
	private final transient List<String> details;

	public BusinessException(ErrorCode code, Object... args) {
		this(code, null, List.of(), args);
	}

	private BusinessException(ErrorCode code, String field, List<String> details, Object[] args) {
		super(Messages.format(code.message(), args));
		this.code = code;
		this.fieldErrors = field == null ? List.of() : List.of(new ApiError.FieldError(field, getMessage()));
		this.details = details;
	}

	public static BusinessException field(ErrorCode code, String field, Object... args) {
		return new BusinessException(code, field, List.of(), args);
	}

	public static BusinessException details(ErrorCode code, List<String> details, Object[] args) {
		return new BusinessException(code, null, details, args);
	}

	public ErrorCode getCode() {
		return code;
	}

	public List<ApiError.FieldError> getFieldErrors() {
		return fieldErrors;
	}

	public List<String> getDetails() {
		return details;
	}

}
