package ardc.cerium.oai.exception;

public class NoSetHierarchyException extends OAIException {

	public NoSetHierarchyException() {
		super();
	}

	@Override
	public String getMessageID() {
		return "oai.error.no-set-hierarchy";
	}

	@Override
	public String getCode() {
		return OAIException.noSetHierarchyCode;
	}

}
