package ardc.cerium.igsn.validator;

import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.exception.XMLValidationException;

import java.io.IOException;

public class ContentValidator {

	private final SchemaService service;

	public ContentValidator(SchemaService service) {
		this.service = service;
	}

	public boolean validate(String content) throws IOException, XMLValidationException {
		return service.validate(content);
	}

}
