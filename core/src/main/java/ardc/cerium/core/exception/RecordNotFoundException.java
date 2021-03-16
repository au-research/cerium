package ardc.cerium.core.exception;

/**
 * Exception for when a Record is not found
 */
public class RecordNotFoundException extends APIException {

	private final String id;

	public RecordNotFoundException(String uuid) {
		super();
		this.id = uuid;
	}

	@Override
	public String getMessageID() {
		return "api.error.record-not-found";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.id };
	}

}
