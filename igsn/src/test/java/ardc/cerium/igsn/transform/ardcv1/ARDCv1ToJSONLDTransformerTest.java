package ardc.cerium.igsn.transform.ardcv1;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.common.util.Helpers;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SchemaService.class })
class ARDCv1ToJSONLDTransformerTest {

	@Autowired
	SchemaService schemaService;

	@Test
	@DisplayName("Can transform between ardcv1 to jsonld")
	void transform_ardcv1_to_jsonld() throws IOException {
		// setup the transformer, make sure it exists
		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.JSONLD);
		Transformer transformer = (Transformer) TransformerFactory.create(fromSchema, toSchema);
		assertThat(transformer).isNotNull();

		// given a version
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		Version version = TestHelper.mockVersion();
		version.setContent(xml.getBytes());

		// when transform, returns actual
		Version actual = transformer.transform(version);

		// actual assertions
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Version.class);
		assertThat(actual.getSchema()).isEqualTo(SchemaService.JSONLD);
		assertThat(actual.getContent()).isNotNull();
		assertThat(actual.getRequestID()).isNotNull();
		assertThat(actual.getRequestID()).isEqualTo(version.getRequestID());

		// json assertions
		String resultJSON = new String(actual.getContent());
		assertThat(resultJSON).isNotBlank();
		DocumentContext json = JsonPath.parse(resultJSON);

		MatcherAssert.assertThat(json, isJson());
		MatcherAssert.assertThat(json, hasJsonPath("$['@igsn']", equalTo("10273/XX0TUIAYLV")));
		MatcherAssert.assertThat(json, hasJsonPath("$.@id", equalTo("http://hdl.handle.net/10273/XX0TUIAYLV")));
		MatcherAssert.assertThat(json, hasJsonPath("$.description"));
		MatcherAssert.assertThat(json, hasJsonPath("$.description.location"));
	}

}