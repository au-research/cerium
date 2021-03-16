package ardc.cerium.drvs.task;

import ardc.cerium.drvs.service.CollectionValidationService;
import ardc.cerium.drvs.service.DRVSIndexingService;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.entity.Version;

public class ValidateAndIndexRecordTask implements Runnable {

	final Request request;

	final Record record;

	final CollectionValidationService collectionValidationService;

	final DRVSIndexingService drvsIndexingService;

	public ValidateAndIndexRecordTask(Request request, Record record,
									  CollectionValidationService collectionValidationService,
									  DRVSIndexingService drvsIndexingService) {
		this.request = request;
		this.record = record;
		this.collectionValidationService = collectionValidationService;
		this.drvsIndexingService = drvsIndexingService;
	}

	/**
	 * get result, save result as Version and add that to the given Record
	 */
	@Override
	public void run() {
		// datacite xml -> drvs-validation
		Version currentDataCiteForRecord = collectionValidationService.getCurrentDataCiteForRecord(record.getId().toString());
		if (currentDataCiteForRecord != null) {
			collectionValidationService.updateValidationVersion(currentDataCiteForRecord, request);
		}

		// index
		drvsIndexingService.queueRecordById(record.getId());
	}

}
