package ardc.cerium.igsn.event;

import ardc.cerium.core.common.entity.Request;

public class TaskCompletedEvent {

    private String message;

    private Request request;

    public TaskCompletedEvent(String message, Request request) {
        this.message = message;
        this.request = request;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
