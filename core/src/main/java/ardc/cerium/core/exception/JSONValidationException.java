package ardc.cerium.core.exception;

public class JSONValidationException extends APIException {

	private final String msg;

	public JSONValidationException(String msg) {
		super();
		this.msg = msg;
	}

	@Override
	public String getMessageID() {
		return "api.error.invalid_json_payload";
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