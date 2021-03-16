package ardc.cerium.oai.exception;

import ardc.cerium.core.exception.APIException;

public abstract class OAIException extends APIException {

	public static final String badArgumentCode = "badArgument";

	public static final String cannotDisseminateFormatCode = "cannotDisseminateFormat";

	public static final String badResumptionTokenCode = "badResumptionToken";

	public static final String badVerbCode = "badVerb";

	public static final String idDoesNotExistCode = "idDoesNotExist";

	public static final String noRecordsMatchCode = "noRecordsMatch";

	public static final String noMetadataFormatsCode = "noMetadataFormats";

	public static final String noSetHierarchyCode = "noSetHierarchy";

	public OAIException() {
		super();
	}

	public abstract String getCode();

}
