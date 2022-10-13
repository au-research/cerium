package ardc.cerium.mycelium.exception;

import ardc.cerium.core.exception.APIException;

public class Neo4JUnavailableException extends APIException {

    @Override
    public String getMessage() {
        return "Neo4j database in unavailable try again later";
    }

}