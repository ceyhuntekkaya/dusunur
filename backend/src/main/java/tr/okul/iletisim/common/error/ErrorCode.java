package tr.okul.iletisim.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	String name();

	HttpStatus status();

	String message();

}
