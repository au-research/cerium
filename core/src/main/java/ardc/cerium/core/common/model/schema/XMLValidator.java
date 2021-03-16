package ardc.cerium.core.common.model.schema;

import ardc.cerium.core.exception.XMLValidationException;
import ardc.cerium.core.common.model.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

public class XMLValidator implements SchemaValidator {

	Logger logger = LoggerFactory.getLogger(XMLValidator.class);

	public boolean validate(Schema schema, String xmlString) {
		try {
			XMLSchema xmlSchema = (XMLSchema) schema;
			String XSDPath = xmlSchema.getLocalSchemaLocation();
			logger.debug("Validating XML String with schema {} schemaLocation: {}", schema.getId(), XSDPath);
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			javax.xml.validation.Schema validationSchema = factory
					.newSchema(getClass().getClassLoader().getResource(XSDPath));
			Validator validator = validationSchema.newValidator();
			StringReader reader = new StringReader(xmlString);
			validator.validate(new StreamSource(reader));
			return true;
		}
		catch (IOException | SAXException e) {
			throw new XMLValidationException(e.getMessage());
		}
	}

}
