package ardc.cerium.drvs.task;

import ardc.cerium.drvs.client.DataCiteClient;
import ardc.cerium.drvs.event.DRVSRecordUpdatedEvent;
import ardc.cerium.drvs.exception.DOINotFoundException;
import ardc.cerium.drvs.exception.DataCiteClientException;
import ardc.cerium.drvs.service.DOIHarvestService;
import ardc.cerium.drvs.service.DRVSIndexingService;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

public class HarvestDOITask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(HarvestDOITask.class);

	private final Request request;

	private final DOIHarvestService doiHarvestService;

	private final DRVSIndexingService drvsIndexingService;

	private final ApplicationEventPublisher applicationEventPublisher;

	private DataCiteClient dataCiteClient;

	public HarvestDOITask(Request request, DOIHarvestService doiHarvestService,
						  DRVSIndexingService drvsIndexingService, ApplicationEventPublisher applicationEventPublisher) {
		this.request = request;
		this.doiHarvestService = doiHarvestService;
		this.drvsIndexingService = drvsIndexingService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * fetch the identifiers, find out if their records have a DOI Identifier as well
	 * fetch the DOI from DataCite store DOI version against the record set version
	 * to current fire up an event to run a MetadataQuality check Task
	 */
	@Override
	public void run() {
		dataCiteClient = new DataCiteClient(doiHarvestService.getDataciteApiUrl());
		request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, 0);
		request.setAttribute(Attribute.NUM_OF_ERROR, 0);
		if (request.getAttribute(Attribute.RECORD_ID) != null) {
			harvestDOIForRecord(request.getAttribute(Attribute.RECORD_ID));
		}
		else {
			harvestDOIsForAllocation(request.getAllocationID().toString());
		}
		doiHarvestService.finalizeRequest(request);
	}

	/**
	 * Imports latest DataCite XML for all DOIs that belongs to records with the given
	 * allocation
	 * @param allocationID
	 */
	private void harvestDOIsForAllocation(String allocationID) {
		List<Identifier> identifiers = doiHarvestService.getDOIsForAllocation(allocationID);
		request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, identifiers.size());
		for (Identifier identifier : identifiers) {
			harvestDataCiteVersion(identifier);
		}
	}

	/**
	 * Imports latest DataCite XML for the DOI that belongs to the given Record
	 * @param recordID
	 */
	private void harvestDOIForRecord(String recordID) {
		request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_RECEIVED);
		Identifier identifier = doiHarvestService.getDOIForRecord(recordID);
		if (identifier == null || identifier.getValue().trim().isEmpty()) {
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			logger.error(String.format("No Identifier found for recordID %s", recordID));
			doiHarvestService.getLoggerFor(request).error("No Identifier found for recordID {}", recordID);
			return;
		}
		harvestDataCiteVersion(identifier);
	}

	/**
	 * @param identifier imports the latest DataCite XML for a given DOI if
	 * DataCiteClientException occurs it will save the error message in the Version
	 * content this way we know if the Metadata was harvested unsuccessfully
	 *
	 */
	private void harvestDataCiteVersion(Identifier identifier) {
		try {
			String doiMetadata = dataCiteClient.getDOIMetadata(identifier.getValue());
			doiHarvestService.updateDataCiteVersion(identifier, doiMetadata, DOIHarvestService.DataCiteXML, request,
					true);
			logger.info("Successfully Harvested DataCite XML metadata for Identifier {}", identifier.getValue());
			doiHarvestService.getLoggerFor(request)
					.info("Successfully Harvested DataCite XML metadata for Identifier {}", identifier.getValue());
		}
		catch (DOINotFoundException e) {
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			// args[2] contains the server message comes back from DataCite
			String[] serverMsg = e.getArgs();
			doiHarvestService.updateDataCiteVersion(identifier, serverMsg[2], DOIHarvestService.DataCiteXML, request,
					false);
			logger.error("Identifier {} failed harvesting due to {}", identifier.getValue(), e.getMessage());
			doiHarvestService.getLoggerFor(request).error("Identifier {} failed harvesting due to {}",
					identifier.getValue(), e.getMessage());
		}
		catch (DataCiteClientException e) {
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			logger.error("Identifier {} failed harvesting due to {}", identifier.getValue(), e.getMessage());
			doiHarvestService.getLoggerFor(request).error("Identifier {} failed importing due to {}",
					identifier.getValue(), e.getMessage());
		}
		catch (Exception e) {
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			logger.error("Identifier {} failed harvesting due to {}", identifier.getValue(), e.getMessage());
			doiHarvestService.getLoggerFor(request).error("Identifier {} failed importing due to {}",
					identifier.getValue(), e.getMessage());
		}finally {
			applicationEventPublisher.publishEvent(new DRVSRecordUpdatedEvent(identifier.getRecord(), request));
		}

	}

}
