package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.*;
import ardc.cerium.core.common.provider.FragmentProvider;
import ardc.cerium.core.common.provider.IdentifierProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.igsn.model.IGSNAllocation;
import ardc.cerium.igsn.model.IGSNTask;
import ardc.cerium.igsn.service.IGSNRegistrationService;
import ardc.cerium.igsn.service.IGSNRequestService;
import ardc.cerium.igsn.service.ImportService;
import ardc.cerium.igsn.task.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.james.mime4j.field.datetime.DateTime;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNService {

	public static final String EVENT_MINT = "igsn.mint";

	public static final String EVENT_BULK_MINT = "igsn.bulk-mint";

	public static final String EVENT_UPDATE = "igsn.update";

	public static final String EVENT_BULK_UPDATE = "igsn.bulk-update";

	public static final String EVENT_RESERVE = "igsn.reserve";

	public static final String EVENT_TRANSFER = "igsn.transfer";

	public static final String IGSNallocationType = "urn:ardc:igsn:allocation";

	public static final String IGSNRecordType = "IGSN";

	private static final Logger logger = LoggerFactory.getLogger(IGSNService.class);

	ThreadPoolExecutor syncIGSNExecutor;

	@Autowired
	SchemaService schemaService;

	@Autowired
	VersionService versionService;

	@Autowired
	IGSNRequestService igsnRequestService;

	@Autowired
	ApplicationEventPublisher applicationEventPublisher;

	private Map<UUID, ThreadPoolExecutor> importExecutors = new HashMap<>();

	@Autowired
	private ImportService importService;


	@Autowired
	private IGSNRegistrationService igsnRegistrationService;

	@PostConstruct
	public void init() {
		importExecutors = new HashMap<>();
	}

	public void queueSync(Identifier identifier, Request request) {
		if (syncIGSNExecutor == null) {
			syncIGSNExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		}

		syncIGSNExecutor
				.execute(new SyncIGSNTask(identifier, request, igsnRegistrationService, applicationEventPublisher, igsnRequestService));
	}

	public void queueImport(UUID allocationID, String identifierValue, File file, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new ImportIGSNTask(identifierValue, file, request, importService, applicationEventPublisher, igsnRequestService));
	}

	public void queueReserve(UUID allocationID, String identifierValue, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new ReserveIGSNTask(identifierValue, request, importService, applicationEventPublisher, igsnRequestService));
	}


	public void queueTransfer(UUID allocationID, String identifierValue, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new TransferIGSNTask(identifierValue, request, importService, applicationEventPublisher, igsnRequestService));
	}

	public void queueUpdate(UUID allocationID, String identifierValue, File file, Request request) {
		if (!importExecutors.containsKey(allocationID)) {
			importExecutors.put(allocationID, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));
		}

		importExecutors.get(allocationID)
				.execute(new UpdateIGSNTask(identifierValue, file, request, importService, applicationEventPublisher, igsnRequestService));
	}

	public void queueIGSNTransformer(Identifier identifier, String fromSchema, String toSchema, Map<String, String> parameters) {
		if (syncIGSNExecutor == null) {
			syncIGSNExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		}
		syncIGSNExecutor
				.execute(new IGSNTransformerTask(identifier, versionService, schemaService, fromSchema, toSchema, parameters));
	}




	// todo check if there's any additional tasks in the request and init finalize if
	// there's none

	/**
	 * a request is considered finished if there are no tasks in the importQueue for that
	 * allocationID, and no more syncTask for that request
	 * @param allocationID the UUID of the Allocation
	 * @param request the {@link Request} in question
	 * @return true if the request is considered finished
	 */
	public boolean isImportInQueue(UUID allocationID , @NotNull Request request) {
		//


		// early return if there's no queue setup
		if (!importExecutors.containsKey(allocationID)) {
			return false;
		}

		// early return if there's a queue but nothing in it
		if (importExecutors.get(allocationID).getQueue().isEmpty()) {
			return false;
		}

		// it has tasks in importqueue queue if there's any IGSNTask with a request ID
		// matching

		BlockingQueue<Runnable> queue = importExecutors.get(allocationID).getQueue();
		return Arrays.stream(queue.toArray()).anyMatch(runnable -> {
			try {
				IGSNTask task = (IGSNTask) runnable;
				return task.getRequestID().equals(request.getId());
			}
			catch (Exception e) {
				logger.error(e.getMessage());
				return false;
			}
		});

	}


	public boolean isSyncInQueue(Request request){
		// it has tasks in sync queue if there's any SyncIGSNTask with a request ID
		// matching
		try {
			if(syncIGSNExecutor.getQueue().isEmpty()){
				return false;
			}

			boolean hasTasksInSyncQueue = syncIGSNExecutor.getQueue().stream().anyMatch(runnable -> {
				IGSNTask task = (IGSNTask) runnable;
				return task.getRequestID().equals(request.getId());
			});
			return hasTasksInSyncQueue;
		}catch(Exception e) {
			return false;
		}
	}

	/**
	 * @param allocationID the allocation ID in this request
	 * @param request the Request
	 * @return boolean true if there are still runnings tasks for this request in any of the queues
	 */
	public boolean isRequestStillRunning(UUID allocationID , @NotNull Request request){
		boolean isImportinQueue = isImportInQueue(allocationID, request);
		boolean isSyncinQueue = isSyncInQueue(request);
		return isImportinQueue || isSyncinQueue;
	}

	/** check if request needs to be COMPLETED or just updated
	 * @param request the Request that has tasks in the queues
	 */
	public void checkRequest(Request request) {
		request = igsnRequestService.save(request);
		UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));
		if (isRequestStillRunning(allocationID, request)) {
			updateRequest(request);
		}else{
			// nothing in the Queues !! BUT
			// sometimes queues are empty because some records in the payload had errors
			// check if the created count and updated count plus error count is less than received records counter
			// if so the request is most likely still being processed but with a lots of errors
			int nReceived = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
			int nErrors = new Integer(request.getAttribute(Attribute.NUM_OF_ERROR));
			int nCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_CREATED));
			int nUpdated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_UPDATED));
			if(nReceived > (nCreated + nUpdated + nErrors))
			{
				updateRequest(request);
			}
			else {
				// finalize the request
				finalizeRequest(request);
				// Also if no other Requests are running using this Allocation
				// shutdown the import Queue (& de-reference) to prevent importExecutors from
				// running
				if(importExecutors.containsKey(allocationID) &&
						importExecutors.get(allocationID).getQueue().isEmpty()){
					importExecutors.get(allocationID).shutdown();
					importExecutors.remove(allocationID);
				}
			}
		}
	}

	public void updateRequest(Request request){
		request.setSummary(getSummaryText(request));
		igsnRequestService.save(request);
	}

	public void finalizeRequest(@NotNull Request request) {
		int numCreated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_CREATED));
		int numUpdated = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_UPDATED));
		int numRegistered = new Integer(request.getAttribute(Attribute.NUM_OF_IGSN_REGISTERED));
		int nReceived = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
		int nErrors = new Integer(request.getAttribute(Attribute.NUM_OF_ERROR));
		if((numCreated + numUpdated + numRegistered) > 0){
			request.setStatus(Request.Status.COMPLETED);
			if(!(request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_MINT) || request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_UPDATE))){
				if(nErrors > 0){
					request.setMessage("Request completed with some errors");
				}else{
					request.setMessage("Request completed successfully");
				}
			}
		}
		else{
			request.setStatus(Request.Status.FAILED);
			if(!(request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_MINT) || request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_UPDATE)))
			{
				request.setMessage("Request failed");
			}
		}
		updateRequest(request);
		igsnRequestService.closeLoggerFor(request);
	}

	private String getSummaryText(Request request){
		Map<String, String> attributes = request.getAttributes();
		StringBuilder summaryText = new StringBuilder();
		long runningTime = request.getUpdatedAt().getTime() - request.getCreatedAt().getTime();
		long diffSeconds = runningTime / 1000 % 60;
		long diffMinutes = runningTime / (60 * 1000) % 60;
		long diffHours = runningTime / (60 * 60 * 1000);
		summaryText.append(String.format("TOTAL TIME: %dh %dm %ds, ", diffHours, diffMinutes, diffSeconds));

		if(request.getAttribute(Attribute.START_TIME_CHUNKING) != null && request.getAttribute(Attribute.END_TIME_CHUNKING) != null){
			runningTime = new Long(request.getAttribute(Attribute.END_TIME_CHUNKING)) - new Long(request.getAttribute(Attribute.START_TIME_CHUNKING));
			diffSeconds = runningTime / 1000 % 60;
			diffMinutes = runningTime / (60 * 1000) % 60;
			diffHours = runningTime / (60 * 60 * 1000);
			summaryText.append(String.format("PROCESS TIME: %dh %dm %ds, ", diffHours, diffMinutes, diffSeconds));
		}
		if(request.getAttribute(Attribute.START_TIME_IMPORT) != null && request.getAttribute(Attribute.END_TIME_IMPORT) != null){
			runningTime = new Long(request.getAttribute(Attribute.END_TIME_IMPORT)) - new Long(request.getAttribute(Attribute.START_TIME_IMPORT));
			diffSeconds = runningTime / 1000 % 60;
			diffMinutes = runningTime / (60 * 1000) % 60;
			diffHours = runningTime / (60 * 60 * 1000);
			summaryText.append(String.format("IMPORT TIME: %dh %dm %ds, ", diffHours, diffMinutes, diffSeconds));
		}
		if(request.getAttribute(Attribute.START_TIME_UPDATE) != null && request.getAttribute(Attribute.END_TIME_UPDATE) != null){
			runningTime = new Long(request.getAttribute(Attribute.END_TIME_UPDATE)) - new Long(request.getAttribute(Attribute.START_TIME_UPDATE));
			diffSeconds = runningTime / 1000 % 60;
			diffMinutes = runningTime / (60 * 1000) % 60;
			diffHours = runningTime / (60 * 60 * 1000);
			summaryText.append(String.format("UPDATE TIME: %dh %dm %ds, ", diffHours, diffMinutes, diffSeconds));
		}
		if(request.getAttribute(Attribute.START_TIME_REGISTER) != null && request.getAttribute(Attribute.END_TIME_REGISTER) != null){
			runningTime = new Long(request.getAttribute(Attribute.END_TIME_REGISTER)) - new Long(request.getAttribute(Attribute.START_TIME_REGISTER));
			diffSeconds = runningTime / 1000 % 60;
			diffMinutes = runningTime / (60 * 1000) % 60;
			diffHours = runningTime / (60 * 60 * 1000);
			summaryText.append(String.format("REGISTER TIME: %dh %dm %ds, ", diffHours, diffMinutes, diffSeconds));
		}


		for(Map.Entry attribute : attributes.entrySet())
		{
			if(attribute.getKey().toString().startsWith("NUM_OF") && attribute.getValue() != null){
				String item = attribute.getKey().toString().replace("NUM_OF", "").replace("_", " ");
				summaryText.append(String.format("%s:%s,", item, attribute.getValue()));
			}
		}
		return summaryText.toString();
	}

	public void shutdownSync() {
		syncIGSNExecutor.shutdown();
		syncIGSNExecutor = null;
	}

	/**
	 * Check if a certain type of task is already in the queue. Uses
	 * {@link IGSNTask#equals(Object)} for comparison
	 * @param allocationID the {@link UUID} of the Allocation that we'll check in
	 * @param taskType the String taskType
	 * @param identifierValue the Identifier Value
	 * @return true if the same IGSNTask acting on the same Identifier is found
	 */
	public boolean hasIGSNTaskQueued(UUID allocationID, String taskType, String identifierValue) {

		// early return if there's no queue setup
		if (!importExecutors.containsKey(allocationID)) {
			return false;
		}

		// early return if there's a queue but nothing in it
		if (importExecutors.get(allocationID).getTaskCount() == 0) {
			return false;
		}

		// check the queue content for import task with identical identifierValue
		BlockingQueue<Runnable> queue = importExecutors.get(allocationID).getQueue();
		return Arrays.stream(queue.toArray()).anyMatch(runnable -> {
			try {
				ImportIGSNTask task = (ImportIGSNTask) runnable;
				return task.getIdentifierValue().equals(identifierValue);
			}
			catch (Exception e) {
				logger.error(e.getMessage());
				return false;
			}
		});
	}

	/**
	 * Chunk and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processMintOrUpdate(@NotNull Request request) {

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		String dataPath = request.getAttribute(Attribute.DATA_PATH);
		String chunkedPayloadPath = dataPath + File.separator + "chunks";
		org.apache.logging.log4j.core.Logger requestLogger = igsnRequestService.getLoggerFor(request);

		request.setAttribute(Attribute.START_TIME_CHUNKING, new Date().getTime());



		request.setStatus(Request.Status.RUNNING);
		// read the payload
		String payload = "";
		try {
			requestLogger.debug("Reading payload at {}", payloadPath);
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}

		// chunk the payload and queue an import task per payload
		try {
			UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));
			Schema schema = schemaService.getSchemaForContent(payload);
			String fileExtension = Helpers.getFileExtensionForContent(payload);

			// create required provider
			FragmentProvider fragmentProvider = (FragmentProvider) MetadataProviderFactory.create(schema,
					Metadata.Fragment);
			IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
					Metadata.Identifier);

			// todo check if chunkedPayloadPath is created properly
			Files.createDirectories(Paths.get(chunkedPayloadPath));
			requestLogger.debug("Created chunked directory at {}", chunkedPayloadPath);

			int count = fragmentProvider.getCount(payload);
			request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, count);
			request.setMessage(String.format("Processing %d records", count));
			requestLogger.debug("Found {} fragments in payload", count);
			for (int i = 0; i < count; i++) {
				String content = fragmentProvider.get(payload, i);
				String outFilePath = chunkedPayloadPath + File.separator + i + fileExtension;
				Helpers.writeFile(outFilePath, content);
				requestLogger.debug("Written payload {} to {}", i, outFilePath);

				// queue the job
				String identifierValue = identifierProvider.get(content);
				String taskType = IGSNTask.TASK_IMPORT;
				if (request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_MINT)
						|| request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_BULK_MINT)) {
					queueImport(allocationID, identifierValue, new File(outFilePath), request);
				}
				else if (request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_UPDATE)
						|| request.getType().equals(ardc.cerium.igsn.service.IGSNService.EVENT_BULK_UPDATE)) {
					queueUpdate(allocationID, identifierValue, new File(outFilePath), request);
				}

				logger.info("Queued task {} for Identifier: {}", taskType, identifierValue);
			}
			igsnRequestService.save(request);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
		request.setAttribute(Attribute.END_TIME_CHUNKING, new Date().getTime());
	}

	/**
	 * Read indvividual Identifiers from the payload and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processReserve(@NotNull Request request) {

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		org.apache.logging.log4j.core.Logger requestLogger = igsnRequestService.getLoggerFor(request);

		request.setAttribute(Attribute.START_TIME_CHUNKING, new Date().getTime());

		request.setStatus(Request.Status.RUNNING);
		// read the payload
		String payload = "";
		try {
			requestLogger.debug("Reading payload at {}", payloadPath);
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
		String schemaId = request.getAttribute(Attribute.SCHEMA_ID);


		// chunk the payload and queue an import task per payload
		try {
			UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));

			Schema schema = schemaService.getSchemaByID(schemaId);
			IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
					Metadata.Identifier);

			List<String> identifiers = identifierProvider.getAll(payload);
			request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, identifiers.size());
			requestLogger.debug("Found {} fragments in payload", identifiers.size());
			for (String identifierValue : identifiers) {
				String taskType = IGSNTask.TASK_RESERVE;
				queueReserve(allocationID, identifierValue, request);
				logger.info("Queued task {} for Identifier: {}", taskType, identifierValue);
			}
			igsnRequestService.save(request);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
		request.setAttribute(Attribute.END_TIME_CHUNKING, new Date().getTime());
	}


	/**
	 * Read indvividual Identifiers from the payload and Queue the Request. Specifically used for bulk requests.
	 * @param request the {@link Request} to chunk and queue if necessary.
	 */
	@Async
	public void processTransfer(@NotNull Request request) {

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		org.apache.logging.log4j.core.Logger requestLogger = igsnRequestService.getLoggerFor(request);

		request.setAttribute(Attribute.START_TIME_CHUNKING, new Date().getTime());

		request.setStatus(Request.Status.RUNNING);
		// read the payload
		String payload = "";
		try {
			requestLogger.debug("Reading payload at {}", payloadPath);
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			logger.error(e.getMessage());
		}
		String schemaId = request.getAttribute(Attribute.SCHEMA_ID);


		// chunk the payload and queue an import task per payload
		try {
			UUID allocationID = UUID.fromString(request.getAttribute(Attribute.ALLOCATION_ID));

			Schema schema = schemaService.getSchemaByID(schemaId);
			IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
					Metadata.Identifier);

			List<String> identifiers = identifierProvider.getAll(payload);
			request.setAttribute(Attribute.NUM_OF_RECORDS_RECEIVED, identifiers.size());
			requestLogger.debug("Found {} fragments in payload", identifiers.size());
			for (String identifierValue : identifiers) {
				String taskType = IGSNTask.TASK_TRANSFER;
				queueTransfer(allocationID, identifierValue, request);
				logger.info("Queued task {} for Identifier: {}", taskType, identifierValue);
			}
			igsnRequestService.save(request);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
		request.setAttribute(Attribute.END_TIME_CHUNKING, new Date().getTime());
	}

	/**
	 * Return the {@link IGSNAllocation}. The Allocation is extracted with first
	 * Identifier in the Content with {@link IdentifierProvider} and the User's Allocation
	 * @param content the XML String content to extract data from.
	 * @param user the {@link User} in the Request, with all of their Allocation and Scope
	 * @param scope the {@link Scope} to check with, usually Scope.Create or Scope.Update
	 * @return the {@link IGSNAllocation} that the User has the scoped access for
	 */
	public IGSNAllocation getIGSNAllocationForContent(String content, @NotNull User user, Scope scope) {
		// obtain the first Identifier
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider provider = (IdentifierProvider) MetadataProviderFactory.create(schema, Metadata.Identifier);
		List<String> identifiers = provider.getAll(content);
		String firstIdentifier = identifiers.get(0);
		return getIGSNAllocationForIdentifier(firstIdentifier, user, scope);
	}

	/**
	 * Return the {@link IGSNAllocation}. The Allocation is extracted with first
	 * Identifier in the Content with {@link IdentifierProvider} and the User's Allocation
	 * @param identifiervalue the XML String content to extract data from.
	 * @param user the {@link User} in the Request, with all of their Allocation and Scope
	 * @param scope the {@link Scope} to check with, usually Scope.Create or Scope.Update
	 * @return the {@link IGSNAllocation} that the User has the scoped access for
	 */
	public IGSNAllocation getIGSNAllocationForIdentifier(String identifiervalue, @NotNull User user, Scope scope) {
		// for each IGSN typed Allocation that the User has access to, find the Allocation
		// that has the prefix and namespace matches this first Identifier
		// todo refactor with Java8 Streaming API for performance
		List<Allocation> allocations = user.getAllocationsByType(IGSNallocationType);
		for (Allocation allocation : allocations) {
			IGSNAllocation ia = (IGSNAllocation) allocation;
			String prefix = ia.getPrefix();
			String namespace = ia.getNamespace();
			if (identifiervalue.startsWith(prefix + "/" + namespace) && ia.getScopes().contains(scope)) {
				return ia;
			}
		}
		// if we got this far it means we didn't find an allocation
		// it maybe an Identifier that doesn't contain the prefix
		// special case we might need to rething CSIROv3 validation
		if(!identifiervalue.contains("/")){
			for (Allocation allocation : allocations) {
				IGSNAllocation ia = (IGSNAllocation) allocation;
				String namespace = ia.getNamespace();
				if (identifiervalue.startsWith(namespace) && ia.getScopes().contains(scope)) {
					return ia;
				}
			}
		}
		return null;
	}


}
