package ardc.cerium.core.common.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Map;

public class HashMapAttributeConverter implements AttributeConverter<Map<String, String>, String> {

	@Override
	public String convertToDatabaseColumn(Map<String, String> attributeMap) {
		String jsonString = null;
		try {
			jsonString = new ObjectMapper().writeValueAsString(attributeMap);
		}
		catch (final JsonProcessingException e) {
			e.printStackTrace();
		}

		return jsonString;
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String s) {
		Map<String, String> attributes = null;
		try {
			attributes = new ObjectMapper().readValue(s, Map.class);
		}
		catch (final IOException e) {

		}
		return attributes;
	}

}
