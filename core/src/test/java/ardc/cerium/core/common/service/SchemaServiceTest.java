package ardc.cerium.core.common.service;

import ardc.cerium.core.exception.XMLValidationException;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.model.schema.JSONSchema;
import ardc.cerium.core.common.model.schema.XMLSchema;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchemaService.class)
class SchemaServiceTest {

	Logger logger = LoggerFactory.getLogger(SchemaServiceTest.class);

	@Autowired
	SchemaService service;

	@Test
	void load() throws Exception {
		service.loadSchemas();
		assertThat(service.getSchemas()).isNotNull();
	}

	@Test
	void getSchemas() {
		// schemas are loaded @PostConstruct so all should be available
		assertThat(service.getSchemas()).extracting("class").contains(JSONSchema.class, XMLSchema.class);
	}

	@Test
	void getSchemaByID() {
		assertThat(service.getSchemaByID(SchemaService.ARDCv1)).isNotNull();
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		assertThat(schema).isInstanceOf(Schema.class);
		assertThat(schema.getName()).isNotNull();
	}

	@Test
	void supports() {
		assertThat(service.supportsSchema("ardc-igsn-desc-1.0")).isTrue();
		assertThat(service.supportsSchema("csiro-igsn-desc-3.0")).isTrue();
		assertThat(service.supportsSchema("igsn-desc-1.0")).isTrue();
		assertThat(service.supportsSchema("igsn-reg-1.0")).isTrue();
		assertThat(service.supportsSchema("non-exist")).isFalse();
	}

	@Test
	void validate_validARDCv1_true() throws Exception {
		Schema schema = service.getSchemaByID(SchemaService.ARDCv1);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		assertTrue(service.validate(schema, validXML));
	}

	@Test
	void validate_validCSIROv3_true() throws Exception {
		Schema schema = service.getSchemaByID(SchemaService.CSIROv3);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		assertTrue(service.validate(schema, validXML));
	}

	@Test
	void getSchemaByNameSpace_ARDC() {
		XMLSchema xs = service.getXMLSchemaByNameSpace("https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc");
		assertEquals(SchemaService.ARDCv1, xs.getId());
	}

	@Test
	void getSchemaByNameSpace_CS() {
		XMLSchema xs = service.getXMLSchemaByNameSpace("https://igsn.csiro.au/schemas/3.0");
		assertEquals(SchemaService.CSIROv3, xs.getId());
	}

	@Test
	void validateDocument_1() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		boolean isValid = service.validate(xml);
		assertTrue(isValid);

	}

	@Test
	void validateDocument_2() throws Exception {
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		boolean isValid = service.validate(xml);
		assertTrue(isValid);

	}


	@Test
	void validate_text_file_with_igsn_list() throws Exception{
		String texfile = Helpers.readFile("src/test/resources/data/igsn.txt");
		boolean isValid = service.validate(texfile);
		assertTrue(isValid);
	}
	// TODO find a better json validator that actually works
	// @Test
	// void validate_validAGNv1_true(){
	// String msg = "";
	// boolean isValid = false;
	// try {
	// Schema schema = service.getSchemaByID(SchemaService.AGNv1);
	// String validJSON = Helpers.readFile("src/test/resources/json/agn-ardc.cerium.core.igsn.json");
	// isValid = service.validate(schema, validJSON);
	// }catch (XMLValidationException e) {
	// msg = e.getMessage();
	// } catch (Exception e) {
	// msg = e.getMessage();
	// }
	// assertTrue(isValid);
	// System.out.println(msg);
	// }

	@Test
	void faileToValidateDocument_3() throws Exception {
		String expectedMassageID = "api.error.invalid_xml_payload";

		boolean isValid = false;
		try {
			String xml = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
			isValid = service.validate(xml);

		}
		catch (XMLValidationException e) {
			String msgID = e.getMessageID();
			assertEquals(msgID, expectedMassageID);
			String msg = e.getMessage();
			assertThat(msg.contains("The content of element 'resource' is not complete"));

		}
	}

	@Test
	void getNamespace() {
		Schema schema1 = service.getSchemaByID(SchemaService.ARDCv1);
		String thenamespace = schema1.getNamespace();
		assertThat(thenamespace).isEqualTo("https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc");
	}

	@Test
	void getSchemaLocation() {
		Schema schema1 = service.getSchemaByID(SchemaService.ARDCv1);
		String theLocation = schema1.getSchemaLocation();
		assertThat(theLocation).isEqualTo("https://identifiers.ardc.edu.au/igsn-schema/descriptive/1.0/resource.xsd");
	}

}