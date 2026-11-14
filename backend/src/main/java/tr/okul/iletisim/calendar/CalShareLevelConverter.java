package tr.okul.iletisim.calendar;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CalShareLevelConverter implements AttributeConverter<CalShareLevel, Short> {

	@Override
	public Short convertToDatabaseColumn(CalShareLevel level) {
		return level == null ? null : (short) level.rank();
	}

	@Override
	public CalShareLevel convertToEntityAttribute(Short value) {
		return value == null ? null : CalShareLevel.ofRank(value);
	}

}
