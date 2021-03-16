package ardc.cerium.oai.provider;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.oai.exception.BadResumptionTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
public class OAIProviderTest {

	@Autowired
	SchemaService schemaService;

	@Test
	void create_Schema_ReturnsInstanceOfOAIProvider() {
		OAIProvider actual = (OAIProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.OAI);
		assertThat(actual).isInstanceOf(OAIProvider.class);
	}

	@Test
	void getNameSpace() {
		Schema schema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		String nameSpace = "https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc";
		OAIProvider actual = (OAIProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.OAI);
		assertThat(actual.getNamespace(schema)).isEqualTo(nameSpace);
	}

	@Test
	void getResumptionToken() throws JsonProcessingException {
		Integer pageSize = 100;
		String resumptionToken = "garbage";
		OAIProvider actual = (OAIProvider) MetadataProviderFactory
				.create(schemaService.getSchemaByID(SchemaService.ARDCv1), Metadata.OAI);
		try {
			String newResumptionToken = actual.getResumptionToken(resumptionToken, pageSize);
		}
		catch (BadResumptionTokenException e) {
			assertThat(e.getCode()).isEqualTo("badResumptionToken");
			assertThat(e.getMessageID()).isEqualTo("oai.error.bad-resumption-token");
		}

	}

}
