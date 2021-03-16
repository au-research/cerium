package ardc.cerium.core.common.transform;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.exception.ContentProviderNotFoundException;
import ardc.cerium.core.exception.TransformerNotFoundException;

public class TransformerFactory {


	public static Object create(Schema fromSchema, Schema toSchema) {
		try {
			String fqdn = fromSchema.getTransforms().get(toSchema.getId());
			return Class.forName(fqdn).newInstance();
		}
		catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new TransformerNotFoundException(fromSchema.getId(), toSchema.getId());
		}
	}



}
