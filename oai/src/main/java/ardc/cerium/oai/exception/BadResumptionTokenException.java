package ardc.cerium.oai.exception;

public class BadResumptionTokenException extends OAIException {

	public BadResumptionTokenException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.bad-resumption-token";
	}

	@Override
	public String getCode() {
		return badResumptionTokenCode;
	}

}
