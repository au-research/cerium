package ardc.cerium.igsn.event;

import ardc.cerium.core.common.event.RecordUpdatedEvent;
import ardc.cerium.igsn.service.RecordProcessingService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RecordEventListener {

	Logger logger = LoggerFactory.getLogger(RecordEventListener.class);

	@Autowired
	RecordProcessingService recordProcessingService;

	@Async
	@EventListener
	public void handleRecordUpdated(@NotNull RecordUpdatedEvent event) {
		String userId = "SYSTEM";
		if (event.getUser() != null) {
			userId = event.getUser().getId().toString();
		}
		logger.debug("Event RecordUpdatedEvent raised with record {} and user {}", event.getRecord().getId(), userId);

		recordProcessingService.queueRecord(event.getRecord());
	}

}
