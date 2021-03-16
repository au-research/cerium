package ardc.cerium.core.exception;

public class ContentNotSupportedException extends APIException {

	private final String msg;

	public ContentNotSupportedException(String msg) {
		super();
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		return this.msg;
	}

	@Override
	public String getMessageID() {
		return "api.error.content_not_supported";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.msg };
	}

}