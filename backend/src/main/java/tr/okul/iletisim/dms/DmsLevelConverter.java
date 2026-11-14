package tr.okul.iletisim.dms;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DmsLevelConverter implements AttributeConverter<DmsLevel, Short> {

	@Override
	public Short convertToDatabaseColumn(DmsLevel level) {
		return level == null ? null : (short) level.rank();
	}

	@Override
	public DmsLevel convertToEntityAttribute(Short value) {
		return value == null ? null : DmsLevel.ofRank(value);
	}

}
