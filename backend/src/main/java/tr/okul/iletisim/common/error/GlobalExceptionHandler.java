package tr.okul.iletisim.common.error;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import tr.okul.iletisim.AppProperties;
import tr.okul.iletisim.core.CoreErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private record Known(ErrorCode code, String field) {
	}

	private static final Map<String, Known> KNOWN_CONSTRAINTS = Map.of(
			"ux_user_email", new Known(CoreErrorCode.USER_EMAIL_TAKEN, "email"),
			"ux_branch_name_key", new Known(CoreErrorCode.BRANCH_NAME_TAKEN, "name"),
			"ux_group_name_key", new Known(CoreErrorCode.GROUP_NAME_TAKEN, "name"),
			"ux_season_name", new Known(CoreErrorCode.SEASON_NAME_TAKEN, "name"),
			"ux_dms_space_name", new Known(ConstraintCode.DMS_SPACE_NAME_TAKEN, "name"),
			"ux_dms_folder_name", new Known(ConstraintCode.DMS_NAME_EXISTS, "name"),
			"ux_dms_file_name", new Known(ConstraintCode.DMS_NAME_EXISTS, "name"));

	private enum ConstraintCode implements ErrorCode {

		DMS_SPACE_NAME_TAKEN(HttpStatus.CONFLICT, "Bu adla bir doküman alanı zaten var."),
		DMS_NAME_EXISTS(HttpStatus.CONFLICT, "Bu konumda aynı adda bir öğe zaten var. Başka bir ad seçin.");

		private final HttpStatus status;
		private final String message;

		ConstraintCode(HttpStatus status, String message) {
			this.status = status;
			this.message = message;
		}

		@Override
		public HttpStatus status() {
			return status;
		}

		@Override
		public String message() {
			return message;
		}

	}

	private final AppProperties app;

	public GlobalExceptionHandler(AppProperties app) {
		this.app = app;
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiError> business(BusinessException ex) {
		log.debug("İş kuralı hatası: {}", ex.getCode().name());
		return respond(ex.getCode().status(), ex.getCode().name(), ex.getMessage(), ex.getFieldErrors(),
				ex.getDetails());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fields = new ArrayList<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> fields.add(new ApiError.FieldError(e.getField(), e.getDefaultMessage())));
		ex.getBindingResult().getGlobalErrors()
				.forEach(e -> fields.add(new ApiError.FieldError(e.getObjectName(), e.getDefaultMessage())));
		return validation(fields);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiError> invalidParams(HandlerMethodValidationException ex) {
		List<ApiError.FieldError> fields = new ArrayList<>();
		ex.getParameterValidationResults().forEach(r -> {
			String name = r.getMethodParameter().getParameterName();
			r.getResolvableErrors().forEach(e -> fields.add(new ApiError.FieldError(name, e.getDefaultMessage())));
		});
		return validation(fields);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex) {
		List<ApiError.FieldError> fields = new ArrayList<>();
		for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
			String path = v.getPropertyPath().toString();
			fields.add(new ApiError.FieldError(path.substring(path.lastIndexOf('.') + 1), v.getMessage()));
		}
		return validation(fields);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiError> unsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		log.warn("Desteklenmeyen içerik türü: {}", ex.getContentType());
		return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
				"Desteklenmeyen içerik türü. Sayfayı yenileyip tekrar deneyin.", List.of(), List.of());
	}

	@ExceptionHandler({ MultipartException.class, MissingRequestHeaderException.class })
	ResponseEntity<ApiError> unreadableRequest(Exception ex) {
		log.warn("İstek okunamadı: {}", ex.toString());
		return simple(CommonErrorCode.REQUEST_UNREADABLE);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
		log.debug("İstek gövdesi okunamadı", ex);
		return simple(CommonErrorCode.REQUEST_UNREADABLE);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
		return simple(CommonErrorCode.REQUEST_INVALID_PARAM, ex.getName());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException ex) {
		return simple(CommonErrorCode.REQUEST_INVALID_PARAM, ex.getParameterName());
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	ResponseEntity<ApiError> missingPart() {
		log.warn("İstekte dosya bölümü eksik");
		return simple(CommonErrorCode.FILE_MISSING);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ApiError> tooLarge() {
		return simple(CommonErrorCode.FILE_TOO_LARGE, app.files().dmsMaxMb());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ApiError> optimisticLock() {
		return simple(CommonErrorCode.CONCURRENT_MODIFICATION);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
		String state = sqlState(ex);
		log.debug("Veri bütünlüğü hatası, SQLState={}", state, ex);
		if ("23503".equals(state)) {
			return simple(CommonErrorCode.RECORD_IN_USE);
		}
		if ("23505".equals(state)) {
			String text = rootMessage(ex);
			for (var entry : KNOWN_CONSTRAINTS.entrySet()) {
				if (text.contains(entry.getKey())) {
					ErrorCode code = entry.getValue().code();
					String message = Messages.format(code.message());
					return respond(code.status(), code.name(), message,
							List.of(new ApiError.FieldError(entry.getValue().field(), message)), List.of());
				}
			}
			return simple(CommonErrorCode.DUPLICATE_RECORD);
		}
		return unexpected(ex);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> methodNotAllowed() {
		return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Bu işlem bu adres için desteklenmiyor.",
				List.of(), List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> routeNotFound() {
		return respond(HttpStatus.NOT_FOUND, CommonErrorCode.ROUTE_NOT_FOUND.name(),
				CommonErrorCode.ROUTE_NOT_FOUND.message(), List.of(), List.of());
	}

	@ExceptionHandler({ CannotGetJdbcConnectionException.class, DataAccessResourceFailureException.class })
	ResponseEntity<ApiError> unavailable(Exception ex) {
		log.error("Veritabanına ulaşılamıyor, traceId={}", MDC.get(ErrorResponseWriter.TRACE_ID_KEY), ex);
		return simple(CommonErrorCode.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> accessDenied() {
		return simple(CommonErrorCode.FORBIDDEN);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		String traceId = MDC.get(ErrorResponseWriter.TRACE_ID_KEY);
		log.error("Beklenmeyen hata, traceId={}", traceId, ex);
		return simple(CommonErrorCode.INTERNAL_ERROR, traceId);
	}

	private ResponseEntity<ApiError> validation(List<ApiError.FieldError> fields) {
		return respond(CommonErrorCode.VALIDATION_FAILED.status(), CommonErrorCode.VALIDATION_FAILED.name(),
				CommonErrorCode.VALIDATION_FAILED.message(), fields, List.of());
	}

	private ResponseEntity<ApiError> simple(ErrorCode code, Object... args) {
		return respond(code.status(), code.name(), Messages.format(code.message(), args), List.of(), List.of());
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message,
			List<ApiError.FieldError> fields, List<String> details) {
		if (status.is4xxClientError()) {
			log.debug("{} {}", code, message);
		}
		return ResponseEntity.status(status)
				.body(new ApiError(code, message, fields, details, MDC.get(ErrorResponseWriter.TRACE_ID_KEY)));
	}

	private static String rootMessage(Throwable ex) {
		StringBuilder sb = new StringBuilder();
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t.getMessage() != null) {
				sb.append(t.getMessage()).append(' ');
			}
		}
		return sb.toString();
	}

	private static String sqlState(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql && sql.getSQLState() != null) {
				return sql.getSQLState();
			}
		}
		return null;
	}

}
