package ardc.cerium.core.common.service;

import java.io.IOException;
import java.util.*;

import javax.annotation.PostConstruct;

import ardc.cerium.core.common.model.schema.*;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.core.exception.JSONValidationException;
import ardc.cerium.core.exception.XMLValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.util.Helpers;

import ardc.cerium.core.common.util.XMLUtil;

/**
 * A Service that deals with supported Schema. Upon
 */
@Service
public class SchemaService {

	// useful helper constants
	public static final String ARDCv1 = "ardc-igsn-desc-1.0";

	public static final String JSONLD = "ardc-igsn-desc-1.0-jsonld";

	public static final String IGSNDESCv1 = "igsn-desc-1.0";

	public static final String IGSNREGv1 = "igsn-reg-1.0";

	public static final String CSIROv3 = "csiro-igsn-desc-3.0";

	public static final String AGNv1 = "agn-igsn-desc-1.0";

	public static final String OAIDC = "oai_dc";

	public static final String IGSNList = "igsn_list";

	private static Schema schema;

	protected final String schemaConfigLocation = "schemas/schemas.json";

	Logger logger = LoggerFactory.getLogger(SchemaService.class);

	private List<Schema> schemas;

	/**
	 * Loads all schemas into locally accessible schema Go through the
	 * schemaConfigLocation file and loads map all available schema
	 * @throws Exception read file ardc.cerium.core.exception
	 */
	public void loadSchemas() throws Exception {
		logger.debug("Loading schema configuration from {}", schemaConfigLocation);
		String data = Helpers.readFileOnClassPath("schemas.json");
		logger.debug("Loaded schema configuration, data length: {}", data.length());
		ObjectMapper mapper = new ObjectMapper();
		List<Schema> schemas = Arrays.asList(mapper.readValue(data, Schema[].class));
		logger.debug("Found & registered {} schemas", schemas.size());
		this.setSchemas(schemas);
	}

	@PostConstruct
	public void init() throws Exception {
		loadSchemas();
	}

	/**
	 * Get a Schema by ID
	 * @param schemaID the ID of the supported Schema
	 * @return Schema
	 */
	public Schema getSchemaByID(String schemaID) {
		logger.debug("Load schema by ID {}", schemaID);
		Optional<Schema> found = this.getSchemas().stream().filter(schema -> schema.getId().equals(schemaID))
				.findFirst();

		return found.orElse(null);
	}

	/**
	 * Get a Schema by NameSpace
	 * @param nameSpace the nameSpace of the supported Schema
	 * @return Schema
	 */
	@Cacheable("schema")
	public XMLSchema getXMLSchemaByNameSpace(String nameSpace) throws ContentNotSupportedException {
		XMLSchema xs = null;
		logger.debug("Load schema by nameSpace {}", nameSpace);
		Iterator<Schema> found = this.getSchemas().stream().filter(schema -> schema.getClass().equals(XMLSchema.class))
				.iterator();

		while (found.hasNext()) {
			xs = (XMLSchema) found.next();
			logger.debug("nameSpaces {}", xs.getNamespace());
			if (xs.getNamespace().equals(nameSpace)) {
				return xs;
			}
			xs = null;
		}
		if (xs == null) {
			throw new ContentNotSupportedException("XML for nameSpace: " + nameSpace + " is not supported");
			// return null;
		}
		return xs;
	}

	/**
	 * Tells if a schema by ID is currently supported by the system
	 * @param schemaID String schemaID
	 * @return boolean
	 */
	public boolean supportsSchema(String schemaID) {
		return getSchemaByID(schemaID) != null;
	}

	public List<Schema> getSchemas() {
		return schemas;
	}

	/**
	 * Sets the current schemas in memory
	 * @param schemas a List of Schema
	 */
	public void setSchemas(List<Schema> schemas) {
		this.schemas = schemas;
	}

	/**
	 * Validate a payload given a schema Will autodetect the schema type and spool up a
	 * SchemaValidator accordingly Supports XMLValidator current todo support
	 * JSONValidator
	 * @param schema The Schema to validate against
	 * @param payload the String payload to validate
	 * @return true if validation success
	 * @throws ContentNotSupportedException throws ardc.cerium.core.exception for validator creation and
	 * validation
	 */
	public boolean validate(Schema schema, String payload) throws ContentNotSupportedException {
		// detect type of schema
		// todo refactor ValidatorFactory.getValidator(schema.getClass())
		// logger.debug("schema {}, payload {}", schema, payload);
		SchemaValidator validator = SchemaValidatorFactory.getValidator(schema);
		if (validator == null) {
			throw new ContentNotSupportedException("Validator for schema " + schema.getId() + " is not found");
		}

		return validator.validate(schema, payload);
	}

	/**
	 * Validate by payload without Schema provided gets the schema with the
	 * targetNamespace of document's namespaceURI validate using the validate(schema,
	 * payload) method
	 * @param payload the content either XML or JSON String
	 * @return true is content validates
	 * @throws XMLValidationException when validation ardc.cerium.core.exception
	 * @throws IOException when failing to obtain validator
	 */
	public boolean validate(String payload) throws XMLValidationException, ContentNotSupportedException, IOException {
		try {
			SchemaValidator validator = SchemaValidatorFactory.getValidator(payload);
			if (validator != null && validator.getClass().equals(XMLValidator.class)) {
				String nameSpace = XMLUtil.getNamespaceURI(payload);
				XMLSchema schema = this.getXMLSchemaByNameSpace(nameSpace);
				return validator.validate(schema, payload);
			}
			else if (validator != null && validator.getClass().equals(PlainTextValidator.class)) {
				// we don't have a schema for CSV or plain text just yet
				return validator.validate(null, payload);
			}
			else if (validator != null && validator.getClass().equals(JSONValidator.class)) {
				// TODO get json validation working
				throw new JSONValidationException("JSON Validation is not yetSupported");
			}
		}
		catch (XMLValidationException e) {
			throw new XMLValidationException(e.getMessage());
		}

		return false;
	}

	/**
	 * Get the Schema the given payload content is defined by gets the schema with the
	 * targetNamespace of document's namespaceURI
	 * @param payload the content either XML or JSON String
	 * @return Schema or null if schema not found or supported
	 * @throws ContentNotSupportedException validation ardc.cerium.core.exception
	 */
	public Schema getSchemaForContent(String payload) throws ContentNotSupportedException {

		try {
			SchemaValidator validator = SchemaValidatorFactory.getValidator(payload);
			if (validator.getClass().equals(XMLValidator.class)) {
				String nameSpace = XMLUtil.getNamespaceURI(payload);
				return this.getXMLSchemaByNameSpace(nameSpace);
			}
			else if (validator.getClass().equals(PlainTextValidator.class)) {
				String nameSpace = XMLUtil.getNamespaceURI(payload);
				return this.getSchemaByID(SchemaService.IGSNList);
			}
			else if (validator.getClass().equals(JSONValidator.class)) {
				throw new ContentNotSupportedException("JSON content import is not yet supported");
				// return null;
			}
		}
		catch (IOException e) {
			throw new ContentNotSupportedException("Unable to determine the validator for content");
		}
		catch (Exception e) {
			throw new ContentNotSupportedException(e.getMessage());
		}
		return null;
	}

}
