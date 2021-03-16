package ardc.cerium.core.common.model.schema;

import java.io.IOException;

import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ardc.cerium.core.exception.JSONValidationException;
import ardc.cerium.core.common.util.Helpers;

public class JSONValidator implements SchemaValidator {

	Logger logger = LoggerFactory.getLogger(JSONValidator.class);

	@Override
	public boolean validate(ardc.cerium.core.common.model.Schema schema, String payload) {

		try {
			JSONSchema js = (JSONSchema) schema;
			String schemaStr = Helpers.readFileOnClassPath(js.getLocalSchemaLocation());

			org.json.JSONObject jsonSchema = new org.json.JSONObject(schemaStr);
			org.json.JSONObject jsonSubject = new org.json.JSONObject(payload);
			logger.debug("schemaStr : {} ", jsonSubject);
			org.everit.json.schema.Schema jSchema = SchemaLoader.load(jsonSchema);
			jSchema.validate(jsonSubject);
			return true;
		}
		catch (ValidationException e) {
			logger.error(e.getMessage());
			throw new JSONValidationException(e.getMessage());
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
		catch (JSONException e) {
			logger.error(e.getMessage());
		}
		return false;
	}

}
