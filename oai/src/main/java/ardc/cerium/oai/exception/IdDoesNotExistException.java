package ardc.cerium.oai.exception;

public class IdDoesNotExistException extends OAIException {

	public IdDoesNotExistException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.id-does-not-exist";
	}

	@Override
	public String getCode() {
		return idDoesNotExistCode;
	}

}
