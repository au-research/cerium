package ardc.cerium.drvs.task;

import ardc.cerium.drvs.event.DRVSRecordUpdatedEvent;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.drvs.model.DRVSSubmission;
import ardc.cerium.drvs.service.DRVSImportService;
import ardc.cerium.drvs.util.CSVUtil;
import ardc.cerium.core.exception.ContentNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.List;

/**
* The {@link Runnable} Task used to Import DRVS records
*/
public class ImportDRVSTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ImportDRVSTask.class);

	private final Request request;

	private final DRVSImportService drvsImportService;

	private final ApplicationEventPublisher applicationEventPublisher;

	public ImportDRVSTask(Request request, DRVSImportService drvsImportService, ApplicationEventPublisher applicationEventPublisher) {
		this.request = request;
		this.drvsImportService = drvsImportService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void run() {
		try {
			request.setStatus(Request.Status.RUNNING);
			String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
			String csvContent = Helpers.readFile(payloadPath);
			List<DRVSSubmission> submissions = CSVUtil.readCSV(csvContent);
			request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, submissions.size());

			for (DRVSSubmission submission : submissions) {
				try {
					Record record = drvsImportService.ingest(submission, request);
					logger.info("Ingested {}", submission.getLocalCollectionID());
					// if record is null the content hasn't been modified no event need to be published
					if(record != null){
						applicationEventPublisher.publishEvent(new DRVSRecordUpdatedEvent(record, request));
					}

				}
				catch (Exception e) {
					logger.error("Failed ingesting submission {}. Reason: {}", submission.getLocalCollectionID(), e.getMessage());
				}
			}
		}
		catch (IOException e) {
			logger.error("Problems accessing file: {} cause: {}", e.getMessage(), e.getCause());
			throw new ContentNotSupportedException("File cannot be accessed");
		} finally {
			drvsImportService.finalizeRequest(request);
		}
	}

}
