package ardc.cerium.igsn.task;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.event.RecordUpdatedEvent;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.igsn.event.IGSNUpdatedEvent;
import ardc.cerium.igsn.event.RequestExceptionEvent;
import ardc.cerium.igsn.event.TaskCompletedEvent;
import ardc.cerium.igsn.model.IGSNTask;
import ardc.cerium.igsn.service.IGSNRequestService;
import ardc.cerium.igsn.service.IGSNService;
import ardc.cerium.igsn.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class ReserveIGSNTask extends IGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ReserveIGSNTask.class);

	private final String identifierValue;

	private final Request request;

	private final ImportService importService;

	ApplicationEventPublisher applicationEventPublisher;

	private final IGSNRequestService igsnRequestService;

	public ReserveIGSNTask(String identifierValue, Request request, ImportService importService,
						   ApplicationEventPublisher applicationEventPublisher, IGSNRequestService igsnRequestService) {
		this.identifierValue = identifierValue;
		super.setIdentifierValue(identifierValue);
		super.setRequestID(request.getId());
		this.request = request;
		this.importService = importService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.igsnRequestService = igsnRequestService;
	}

	@Override
	public void run() {
		org.apache.logging.log4j.core.Logger requestLog = igsnRequestService.getLoggerFor(request);
		try {
			logger.info("Processing Reserving Identifier: {}", identifierValue);
			// only set it once
			if(request.getAttribute(Attribute.START_TIME_IMPORT) == null){
				request.setAttribute(Attribute.START_TIME_IMPORT, new Date().getTime());
			}
			Identifier identifier = importService.reserveRequest(identifierValue, request);
			if (identifier != null) {
				int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
				int numCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_CREATED));
				requestLog.info(String.format("Successfully created Record with Identifier: %s , status: %s",
						identifier.getValue(), identifier.getStatus()));
				request.setMessage(String.format("Imported %d out of %d", numCreated, totalCount));
			}
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_CREATED);
			request.setAttribute(Attribute.END_TIME_IMPORT, new Date().getTime());

			String message = String.format("Processed Identifier: %s", identifierValue);
			logger.info(message);
			applicationEventPublisher.publishEvent(new TaskCompletedEvent(message, request));
		}
		catch (ForbiddenOperationException e) {
			requestLog.error(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			// for import the only reason it is forbidden if the record already exists
			request.incrementAttributeValue(Attribute.NUM_OF_RECORD_ALREADY_EXISTS);
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}

	}

	public UUID getRequestID() {
		return request.getId();
	}

}
