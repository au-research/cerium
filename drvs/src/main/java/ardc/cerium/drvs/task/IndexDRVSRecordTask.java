package ardc.cerium.drvs.task;

import ardc.cerium.drvs.service.DRVSIndexingService;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.service.RecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

public class IndexDRVSRecordTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(IndexDRVSRecordTask.class);

	final UUID recordID;

	final DRVSIndexingService indexingService;

	final RecordService recordService;

	Date dateAdded;

	public IndexDRVSRecordTask(UUID recordID, DRVSIndexingService indexingService, RecordService recordService) {
		this.recordID = recordID;
		this.indexingService = indexingService;
		this.recordService = recordService;
		dateAdded = new Date();
	}

	public UUID getRecordID() {
		return recordID;
	}

	public Date getDateAdded() {
		return dateAdded;
	}

	@Override
	public void run() {
		logger.info("Indexing record {}", recordID);
		try {
			Record record = recordService.findById(String.valueOf(recordID));
			indexingService.index(record);
		}
		catch (Exception e) {
			logger.error("Failed to index record {} Reason: {}", recordID, e.getStackTrace());
		}
	}

}
