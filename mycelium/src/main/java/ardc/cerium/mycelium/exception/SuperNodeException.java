package ardc.cerium.mycelium.exception;

import ardc.cerium.core.exception.APIException;

public class SuperNodeException extends APIException {

    private final String roId;

    public SuperNodeException(String roId) {
        super();
        this.roId = roId;
    }

    @Override
    public String[] getArgs() {
        return new String[] { roId };
    }

    @Override
    public String getMessage() {
        return String.format("RegistryObject with id:%s should be processed by the supernode queue",this.roId);
    }

}