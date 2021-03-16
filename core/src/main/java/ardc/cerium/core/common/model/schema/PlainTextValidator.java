package ardc.cerium.core.common.model.schema;

import ardc.cerium.core.common.model.Schema;

public class PlainTextValidator implements SchemaValidator{
    @Override
    public boolean validate(Schema schema, String payload) {
        // if it's a plain text document it should contain one or multiple lines of identifiers
        String[] lines = payload.split("\\r?\\n");
        return lines.length > 0;
    }
}
