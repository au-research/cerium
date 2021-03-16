package ardc.cerium.igsn.provider.ardcv1;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.LandingPageProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1LandingPageProviderTest {

	@Autowired
	SchemaService service;

	@Test
	@DisplayName("Get landing page From ARDCV1")
	public void getLandingPage() throws Exception {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		LandingPageProvider provider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);
		String landingPageValue = provider.get(xml);
		assertEquals(landingPageValue, "https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
	}

}