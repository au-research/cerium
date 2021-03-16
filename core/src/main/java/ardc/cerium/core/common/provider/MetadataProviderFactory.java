package ardc.cerium.core.common.provider;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.exception.ContentProviderNotFoundException;

public class MetadataProviderFactory {

	public static Object create(Schema schema, Metadata metadata) throws ContentProviderNotFoundException {
		try {
			String fqdn = schema.getProviders().get(metadata);
			return Class.forName(fqdn).newInstance();
		}
		catch (NullPointerException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new ContentProviderNotFoundException(schema, metadata);
		}
	}

}
