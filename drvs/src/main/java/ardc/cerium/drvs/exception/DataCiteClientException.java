package ardc.cerium.drvs.exception;

import ardc.cerium.core.exception.APIException;

public class DataCiteClientException extends APIException {

    private final String msg;
    private final String data_cite_url;
    private final String service_url;

    public DataCiteClientException(String data_cite_url, String service_url, String msg) {
        this.msg = msg;
        this.data_cite_url = data_cite_url;
        this.service_url = service_url;
    }

    @Override
    public String getMessageID() {
        return "api.error.mds_client_exception";
    }

    @Override
    public String[] getArgs() {
        return new String[] { this.data_cite_url, this.service_url, this.msg };
    }

    @Override
    public String getMessage() {
        return String.format("Error while connecting to %s%s, msg:%s",this.data_cite_url, this.service_url, this.msg);
    }
}