package ardc.cerium.igsn.transform.ardcv1;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1ToRegistrationMetadataTransformerTest {

	@Autowired
	SchemaService schemaService;

	@Test
	@DisplayName("Can transform between ardcv1 to registration metadata")
	void transform() throws IOException {
		// setup the transformer, make sure it exists
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);
		Transformer transformer = (Transformer) TransformerFactory
				.create(fromSchema, toSchema);

		assertThat(transformer).isNotNull();

		// given a version
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		Version version = TestHelper.mockVersion();
		version.setContent(xml.getBytes());
		/**
		 * The following parameters should be added to the transformer to achieve better
		 * (richer) result <xsl:param name="registrantName" select="'registrantName'"/>
		 * <xsl:param name="nameIdentifier" select="'nameIdentifier'"/>
		 * <xsl:param name="nameIdentifierScheme" select="'nameIdentifierScheme'"/>
		 * <xsl:param name="eventType" select="'eventType'"/>
		 * <xsl:param name="timeStamp" select="'timeStamp'"/>
		 *
		 *
		 */
		transformer.setParam("registrantName", "John Citizen").setParam("nameIdentifier", "100898999-998766")
				.setParam("nameIdentifierScheme", "ORCID").setParam("eventType", "updated")
				.setParam("timeStamp", "2018-06-06T13:45:45.5654+10.00");
		// when transform, returns actual
		Version actual = transformer.transform(version);

		// actual assertions
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Version.class);
		assertThat(actual.getSchema()).isEqualTo(SchemaService.IGSNREGv1);
		assertThat(actual.getContent()).isNotNull();
		assertThat(actual.getRequestID()).isNotNull();
		assertThat(actual.getRequestID()).isEqualTo(version.getRequestID());

		// json assertions
		String content = new String(actual.getContent());
		assertThat(content.contains("<registrantName>John Citizen</registrantName>")).isTrue();
	}

	@Test
	@DisplayName("Test that params are overwritten")
	void setParam() {
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);
		ARDCv1ToRegistrationMetadataTransformer transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory
				.create(fromSchema, toSchema);

		assertThat(transformer).isNotNull();
		transformer.setParam("foo", "bar");
		transformer.setParam("foo", "bart");
		Map<String, String> params = transformer.getParams();
		assertThat(params.size() == 1).isTrue();
		assertThat(params.get("foo").equals("bart")).isTrue();

	}

}