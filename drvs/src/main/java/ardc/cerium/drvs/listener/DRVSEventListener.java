package ardc.cerium.drvs.listener;

import ardc.cerium.drvs.event.DRVSRecordUpdatedEvent;
import ardc.cerium.drvs.service.CollectionValidationService;
import ardc.cerium.drvs.service.DRVSIndexingService;
import ardc.cerium.drvs.task.ValidateAndIndexRecordTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.drvs.enabled")
public class DRVSEventListener {

	final CollectionValidationService collectionValidationService;

	final DRVSIndexingService indexingService;

	Logger logger = LoggerFactory.getLogger(DRVSEventListener.class);

	public DRVSEventListener(CollectionValidationService collectionValidationService,
			DRVSIndexingService indexingService) {
		this.collectionValidationService = collectionValidationService;
		this.indexingService = indexingService;
	}

	@EventListener
	public void handleRecordUpdatedEvent(DRVSRecordUpdatedEvent event) {
		logger.debug("Event handled for record {} and request {}", event.getRecord().getId(),
				event.getRequest().getId());
		(new ValidateAndIndexRecordTask(event.getRequest(), event.getRecord(), collectionValidationService,
				indexingService)).run();
	}

}
