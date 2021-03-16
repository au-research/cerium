package ardc.cerium.oai.exception;

public class CannotDisseminateFormatException extends OAIException {

	public CannotDisseminateFormatException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.cannot-disseminate-format";
	}

	@Override
	public String getCode() {
		return cannotDisseminateFormatCode;
	}

}
