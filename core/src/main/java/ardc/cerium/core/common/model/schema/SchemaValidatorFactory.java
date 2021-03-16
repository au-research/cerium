package ardc.cerium.core.common.model.schema;

import java.io.IOException;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.util.Helpers;

public class SchemaValidatorFactory {

	public static SchemaValidator getValidator(Schema schema) {
		if (schema.getClass().equals(XMLSchema.class)) {
			return new XMLValidator();
		}
		else if (schema.getClass().equals(JSONSchema.class)) {
			return new JSONValidator();
		}

		return null;
	}

	public static SchemaValidator getValidator(String content) throws IOException {
		if (Helpers.probeContentType(content).equals("application/xml")) {
			return new XMLValidator();
		}
		else if (Helpers.probeContentType(content).equals("application/json")) {
			return new JSONValidator();
		}
		else if (Helpers.probeContentType(content).equals("text/plain")) {
			return new PlainTextValidator();
		}
		return null;
	}

}
