package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.igsn.task.ProcessTitleTask;
import ardc.cerium.igsn.task.TransformJSONLDTask;
import ardc.cerium.igsn.task.TransformOAIDCTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

@Service
public class RecordProcessingService {

	private static final Logger logger = LoggerFactory.getLogger(RecordProcessingService.class);

	private ThreadPoolExecutor processQueue;

	@Autowired
	private VersionService versionService;

	@Autowired
	private RecordService recordService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private EntityManager entityManager;

	@PostConstruct
	public void init() {
		processQueue = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
	}

	public void queueRecord(@NotNull Record record) {
		logger.info("Queueing record: {}", record.getId());

		processQueue.execute(new ProcessTitleTask(record, versionService, recordService, schemaService));
		processQueue.execute(new TransformJSONLDTask(record, versionService, schemaService));
		processQueue.execute(new TransformOAIDCTask(record, versionService, schemaService));
	}

	@Transactional(readOnly = true)
	public void queueAllRecords() {
		logger.info("Queueing All Records");
		Stream<Record> bookStream = recordRepository.getAll();

		bookStream.forEach(record -> {
			logger.info("Queueing record: {}", record.getId());
			processQueue.execute(new ProcessTitleTask(record, versionService, recordService, schemaService));
			processQueue.execute(new TransformJSONLDTask(record, versionService, schemaService));
			processQueue.execute(new TransformOAIDCTask(record, versionService, schemaService));
			entityManager.detach(record);
		});
	}

}