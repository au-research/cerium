package ardc.cerium.core.exception;

public class RequestNotFoundException extends APIException {

	private final String id;

	public RequestNotFoundException(String id) {
		super();
		this.id = id;
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.id };
	}

	@Override
	public String getMessageID() {
		return "api.error.request-not-found";
	}

}
