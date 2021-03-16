package ardc.cerium.core.exception;

public class VersionNotFoundException extends APIException {

	private final String id;

	public VersionNotFoundException(String uuid) {
		super();
		this.id = uuid;
	}

	@Override
	public String getMessageID() {
		return "api.error.version-not-found";
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.id };
	}

}
