package ardc.cerium.core.exception;

public class XMLValidationException extends APIException {

	private final String msg;

	public XMLValidationException(String msg) {
		super();
		this.msg = msg;
	}

	@Override
	public String getMessageID() {
		return "api.error.invalid_xml_payload";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.msg };
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

}
