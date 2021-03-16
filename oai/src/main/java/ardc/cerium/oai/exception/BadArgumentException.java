package ardc.cerium.oai.exception;

public class BadArgumentException extends OAIException {

	public BadArgumentException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.bad-argument";
	}

	@Override
	public String getCode() {
		return badArgumentCode;
	}

}
