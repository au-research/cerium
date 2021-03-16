package ardc.cerium.igsn.transform.ardcv1;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1ToOAIDCTransformerTest {

	@Autowired
	SchemaService schemaService;

	@Test
	@DisplayName("Can transform between ardcv1 to ardc.cerium.core.oai-dc")
	void transform() throws IOException {
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.OAIDC);

		// create a transformer
		ARDCv1ToOAIDCTransformer transformer = (ARDCv1ToOAIDCTransformer) TransformerFactory.create(fromSchema,
				toSchema);
		assertThat(transformer).isNotNull();

		// given a version
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		Version version = TestHelper.mockVersion();
		version.setContent(xml.getBytes());

		// when transform, returns actual version with correct content and schem ID
		Version actual = transformer.transform(version);
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Version.class);
		assertThat(actual.getSchema()).isEqualTo(SchemaService.OAIDC);
		assertThat(actual.getContent()).isNotNull();
		assertThat(actual.getRequestID()).isNotNull();
		assertThat(actual.getRequestID()).isEqualTo(version.getRequestID());

		// ensure the result contains some text, maybe extend to xpath testing
		String resultXML = new String(actual.getContent());
		assertThat(resultXML).contains("Classification");
	}

}