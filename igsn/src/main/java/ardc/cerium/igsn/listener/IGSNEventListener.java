package ardc.cerium.igsn.listener;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.igsn.event.IGSNSyncedEvent;
import ardc.cerium.igsn.event.IGSNUpdatedEvent;
import ardc.cerium.igsn.event.RequestExceptionEvent;
import ardc.cerium.igsn.event.TaskCompletedEvent;
import ardc.cerium.igsn.service.IGSNService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNEventListener {

	Logger logger = LoggerFactory.getLogger(IGSNEventListener.class);

	@Autowired
	IGSNService igsnService;

	@EventListener
	public void handleIGSNUpdatedEvent(IGSNUpdatedEvent event) {
		Request request = event.getRequest();
		logger.debug("Event handleIGSNUpdatedEvent raised with identifier {} for request {}",
				event.getIdentifier().getValue(), request.getId());
		if(event.getIdentifier() != null){
			igsnService.queueSync(event.getIdentifier(), request);
		}
		igsnService.checkRequest(request);

	}

	@EventListener
	public void handleIGSNSyncedEvent(IGSNSyncedEvent event) {
		Request request = event.getRequest();
		logger.debug("Event IGSNSyncedEvent raised with identifier {} for request {}", event.getIdentifier().getValue(),
				request.getId());
		if(event.getIdentifier() != null && request.getAttribute(Attribute.ALLOCATION_PREFIX) != null
				&& request.getAttribute(Attribute.SCHEMA_ID).equals(SchemaService.CSIROv3)){
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("prefix", request.getAttribute(Attribute.ALLOCATION_PREFIX));
			igsnService.queueIGSNTransformer(event.getIdentifier(), SchemaService.CSIROv3, SchemaService.ARDCv1, parameters);
		}
		igsnService.checkRequest(request);
	}

	@EventListener
	public void handleIGSNSErrorEvent(RequestExceptionEvent event) {
		Request request = event.getRequest();
		logger.debug("Event RequestExceptionEvent raised with message {} for request {}", event.getMessage(),
				request.getId());
		igsnService.checkRequest(request);
	}

	@EventListener
	public void handleTaskCompletedEvent(TaskCompletedEvent event) {
		Request request = event.getRequest();
		logger.debug("Event handleTaskCompletedEvent raised with message {} for request {}", event.getMessage(),
				request.getId());
		igsnService.checkRequest(request);
	}

}
