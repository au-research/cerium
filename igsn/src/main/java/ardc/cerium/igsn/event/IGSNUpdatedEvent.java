package ardc.cerium.igsn.event;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Request;

public class IGSNUpdatedEvent {

	private Identifier identifier;

	private Request request;

	public IGSNUpdatedEvent(Identifier identifier, Request request) {
		this.identifier = identifier;
		this.request = request;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

}
