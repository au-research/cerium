package ardc.cerium.core.exception;

public class MDSClientConfigurationException extends APIException {

	private String msgID;

	private String allocationName;

	public enum Configuration {

		user_name, password, server_url

	};

	public MDSClientConfigurationException(Configuration config, String allocationName) {
		super();
		if (config.equals(Configuration.password)) {
			msgID = "api.error.mds_client_configuration_password_missing";
		}
		if (config.equals(Configuration.user_name)) {
			msgID = "api.error.mds_client_configuration_username_missing";
		}
		if (config.equals(Configuration.server_url)) {
			msgID = "api.error.mds_client_configuration_server_url_missing";
		}
		this.allocationName = allocationName;
	}

	@Override
	public String[] getArgs() {
		return new String[] { this.allocationName };
	}

	@Override
	public String getMessageID() {
		return msgID;
	}

}