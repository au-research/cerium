package ardc.cerium.core.common.model.schema;

import ardc.cerium.core.common.model.Schema;

public interface SchemaValidator {

	boolean validate(Schema schema, String payload);

}
