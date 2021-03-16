package ardc.cerium.core.exception;

public class MDSClientException extends APIException {

    private final String msg;
    private final String mds_url;
    private final String service_url;

    public MDSClientException(String mds_url, String service_url, String msg) {
        super();
        this.msg = msg;
        this.mds_url = mds_url;
        this.service_url = service_url;
    }

    @Override
    public String getMessageID() {
        return "api.error.mds_client_exception";
    }

    @Override
    public String[] getArgs() {
        return new String[] { this.mds_url, this.service_url, this.msg };
    }

    @Override
    public String getMessage() {
        return String.format("Error while connecting to %s%s, msg:%s",this.mds_url, this.service_url, this.msg);
    }
}