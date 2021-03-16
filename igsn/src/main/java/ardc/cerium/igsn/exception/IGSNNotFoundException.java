package ardc.cerium.igsn.exception;

import ardc.cerium.core.exception.APIException;

public class IGSNNotFoundException extends APIException {

	private final String igsn;

	public IGSNNotFoundException(String igsn) {
		super();
		this.igsn = igsn;
	}

	@Override
	public String[] getArgs() {
		return new String[] { igsn };
	}

	@Override
	public String getMessageID() {
		return "igsn.error.igsn-not-found";
	}

}
