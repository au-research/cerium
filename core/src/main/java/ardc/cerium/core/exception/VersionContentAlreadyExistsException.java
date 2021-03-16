package ardc.cerium.core.exception;

import io.swagger.v3.oas.annotations.Operation;

public class VersionContentAlreadyExistsException extends APIException {

	private final String identifierValue;

	private final String schema;

	public VersionContentAlreadyExistsException(String identifierValue, String schema) {
		super();
		this.identifierValue = identifierValue;
		this.schema = schema;
	}

	@Override
	public String getMessageID() {
		return "api.error.version-already-exist";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.identifierValue, this.schema };
	}

}