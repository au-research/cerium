package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class ARDCv1IdentifierProviderTest {

	@Autowired
	SchemaService service;

	@Test
	public void extractIdentifierFromARDCV1() throws Exception {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		String identifierValue = provider.get(xml);
		assertEquals(identifierValue, "10273/XX0TUIAYLV");
	}

	@Test
	public void extract_3_IdentifiersFromARDCV1() throws Exception {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml");
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		assert provider != null;
		List<String> identifiers = provider.getAll(xml);
		assertEquals(identifiers.size(), 3);
	}

}
