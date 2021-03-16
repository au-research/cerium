package ardc.cerium.drvs.controller;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.drvs.repository.DRVSRecordElasticRepository;
import ardc.cerium.drvs.service.DRVSImportService;
import ardc.cerium.drvs.service.DRVSIndexingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@RestController
@RequestMapping(value = "/api/admin/drvs")
@ConditionalOnProperty(name = "app.drvs.enabled")
public class DRVSAdminController {

	final DRVSRecordElasticRepository elasticsearchRepository;

	final RecordRepository recordRepository;

	final DRVSIndexingService drvsIndexingService;

	public DRVSAdminController(DRVSRecordElasticRepository elasticsearchRepository, RecordRepository recordRepository,
			DRVSIndexingService drvsIndexingService) {
		this.elasticsearchRepository = elasticsearchRepository;
		this.recordRepository = recordRepository;
		this.drvsIndexingService = drvsIndexingService;
	}

	@PostMapping("/reindex")
	public ResponseEntity<?> reindex() {
		drvsIndexingService.ensureIndexExist();

		// clean slate
		elasticsearchRepository.deleteAll();

		List<Record> records = recordRepository.findAllByType(DRVSImportService.DRVS_RECORD_TYPE);
		records.forEach(record -> drvsIndexingService.queueRecordById(record.getId()));

		String message = String.format("Queued %s records for indexing", records.size());
		return ResponseEntity.ok().body(message);
	}

	@GetMapping("/index-queue")
	public ResponseEntity<?> showIndexQueue() {
		Map<String, Object> indexQueue = new HashMap<>();
		BlockingQueue<?> queue = drvsIndexingService.getIndexQueue().getQueue();
		indexQueue.put("size", queue.size());
		indexQueue.put("queue", queue.toArray());
		return ResponseEntity.ok().body(indexQueue);
	}

}
