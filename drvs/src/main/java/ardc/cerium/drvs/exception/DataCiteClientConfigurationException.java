package ardc.cerium.drvs.exception;

import ardc.cerium.core.exception.APIException;

public class DataCiteClientConfigurationException extends APIException {

    private String msgID;


    public enum Configuration {server_url};

    public DataCiteClientConfigurationException(DataCiteClientConfigurationException.Configuration config) {
        super();
        if (config.equals(DataCiteClientConfigurationException.Configuration.server_url)) {
            msgID = "api.error.data_cite_client_configuration_server_url_missing";
        }
    }

    @Override
    public String getMessageID() {
        return msgID;
    }

}