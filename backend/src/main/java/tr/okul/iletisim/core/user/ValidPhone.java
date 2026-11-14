package tr.okul.iletisim.core.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import tr.okul.iletisim.common.util.PhoneNormalizer;

@Documented
@Constraint(validatedBy = ValidPhone.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {

	String message() default "Telefon numarası 05XX XXX XX XX biçiminde olmalıdır.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<ValidPhone, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return PhoneNormalizer.isValid(value);
		}

	}

}
