package ardc.cerium.igsn.validator;

import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchemaService.class)
class ContentValidatorTest {

	@Autowired
	SchemaService schemaService;

	@Test
	@DisplayName("Validate a valid ardcv1 XML payload should return true")
	void validateValidXML() throws IOException {
		ContentValidator contentValidator = new ContentValidator(schemaService);
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		assertTrue(contentValidator.validate(xml));
	}

	@Test
	@DisplayName("Validate an XML with no namespace will throw ContentNotSupportedException")
	void failNoNamespace() throws IOException {
		ContentValidator contentValidator = new ContentValidator(schemaService);
		String xml = Helpers.readFile("src/test/resources/xml/shiporder.xml");
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			contentValidator.validate(xml);
		});
	}

	@Test
	@DisplayName("Validate a rifcs unsupported XML will throw ContentNotSupportedException")
	void failrifcsNameSpace() throws IOException {
		ContentValidator contentValidator = new ContentValidator(schemaService);
		String xml = Helpers.readFile("src/test/resources/xml/rifcs_sample.xml");
		Assert.assertThrows(ContentNotSupportedException.class, () -> {
			contentValidator.validate(xml);
		});
	}

}
