package ardc.cerium.oai.exception;

public class BadVerbException extends OAIException {

	public BadVerbException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.bad-verb";
	}

	@Override
	public String getCode() {
		return OAIException.badVerbCode;
	}

}