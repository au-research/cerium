package ardc.cerium.igsn.task;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.NotChangedException;
import ardc.cerium.igsn.event.RequestExceptionEvent;
import ardc.cerium.igsn.event.TaskCompletedEvent;
import ardc.cerium.igsn.model.IGSNTask;
import ardc.cerium.igsn.service.IGSNRequestService;
import ardc.cerium.igsn.service.ImportService;
import ardc.cerium.igsn.task.ReserveIGSNTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Date;
import java.util.UUID;

public class TransferIGSNTask extends IGSNTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ReserveIGSNTask.class);

	private final String identifierValue;

	private final Request request;

	private final ImportService importService;

	ApplicationEventPublisher applicationEventPublisher;

	private final IGSNRequestService igsnRequestService;

	public TransferIGSNTask(String identifierValue, Request request, ImportService importService,
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
			Identifier identifier = importService.transferRequest(identifierValue, request);
			if (identifier != null) {
				int totalCount = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
				int numCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_UPDATED));
				requestLog.info(String.format("Successfully Transferred Ownership of Record with Identifier: %s",
						identifier.getValue()));
				request.setMessage(String.format("Imported %d out of %d", numCreated, totalCount));
			}
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_UPDATED);
			request.setAttribute(Attribute.END_TIME_IMPORT, new Date().getTime());

			String message = String.format("Processed Identifier: %s", identifierValue);
			logger.info(message);
			applicationEventPublisher.publishEvent(new TaskCompletedEvent(message, request));
		}
		catch (ForbiddenOperationException | NotChangedException e) {
			requestLog.error(e.getMessage());
			logger.warn(e.getMessage());
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			applicationEventPublisher.publishEvent(new RequestExceptionEvent(e.getMessage(), request));
		}

	}

	public UUID getRequestID() {
		return request.getId();
	}

}
