package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.FragmentProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1FragmentProviderTest {

	@Autowired
	SchemaService service;

	@Test
	void get() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml");
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		String first = fProvider.get(xml, 0);
		assertTrue(first.contains("<resourceTitle>This Tiltle also left blank on purpose</resourceTitle>"));
		String third = fProvider.get(xml, 2);
		assertTrue(
				third.contains("<resourceTitle>zircon sample (Sample JS43 / IGSN XXAB0002C) from the Neal McNaughton"));
	}

	@Test
	void getCount() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1_batch.xml");
		FragmentProvider fProvider = (FragmentProvider) MetadataProviderFactory.create(schema, Metadata.Fragment);
		int fragCounter = fProvider.getCount(xml);
		assertTrue(fragCounter == 3);
	}

	@Test
	void get_fragmentOnSameDocumentConsistentResult() throws IOException {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String original = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
				Metadata.Fragment);
		String firstFragmentOnce = fragmentProvider.get(original, 0);
		String firstFragmentAgain = fragmentProvider.get(original, 0);

		assertThat(firstFragmentOnce).isEqualTo(firstFragmentAgain);
	}

}