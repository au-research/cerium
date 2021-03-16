package ardc.cerium.core.exception;

public class ForbiddenOperationException extends APIException {

	private final String msg;

	public ForbiddenOperationException(String msg) {
		super();
		this.msg = msg;
	}

	@Override
	public String getMessageID() {
		return "api.error.forbidden-operation";
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.msg };
	}

}
