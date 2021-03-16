package ardc.cerium.drvs.service;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.ForbiddenOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@ConditionalOnProperty(name = "app.drvs.enabled")
public class DOIHarvestService {

	public static final String DataCiteXML = "datacite-xml";

	private static final Logger logger = LoggerFactory.getLogger(DOIHarvestService.class);

	@Autowired
	RequestService requestService;

	@Autowired
	DRVSVersionService drvsVersionService;

	@Autowired
	private RecordService recordService;

	@Value("${datacite.api_url:https://api.datacite.org/dois/}")
	private String dataciteApiUrl;

	public Request save(Request request) {
		return requestService.save(request);
	}

	/**
	 * Creates a drvs.harvest Request to strore data (in this case no data stored) , and
	 * logs for the request
	 * @param user the User who initiated the harvest Request
	 * @param type single or bulk harvest
	 * @return the new Request
	 */
	public Request createHarvestRequest(User user, String type) {
		Request request = new Request();
		request.setType(type);
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());
		request.setCreatedBy(user.getId());

		request = save(request);

		// create data path
		try {
			Path path = Paths.get(requestService.getDataPathFor(request));
			Files.createDirectories(path);
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			logger.error("Failed creating data path {}", e.getMessage());
		}

		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));

		// upload file path
		String dataPath = requestService.getDataPathFor(request);
		request = save(request);
		return request;
	}

	/**
	 *
	 * Find the DOI (Identifier) for the Given record
	 * @param recordID the recordID to harvest its DOI's DataCite MetaData
	 * @return the DOI Identifier or null
	 */
	public Identifier getDOIForRecord(String recordID) {
		Record record = recordService.findById(recordID);
		if(record == null){
			throw new ForbiddenOperationException(
					String.format("Record with recordID %s doesn't exist", recordID));
		}
		List<Identifier> identifiers = record.getIdentifiers();
		for (Identifier identifier : identifiers) {
			// a Record can have only 1 DOI Identifier
			// so safe to return the first one
			if (identifier.getType().equals(Identifier.Type.DOI) && !identifier.getValue().isEmpty()) {
				return identifier;
			}
		}
		return null;
	}

	/**
	 * Find all the DOIS for all records from the same Allocation
	 * @param allocationID of all Records to harvest their DOI's DataCite MetaData
	 * @return all DOI Identifiers that belong to records with given AllocationID
	 */
	public List<Identifier> getDOIsForAllocation(String allocationID) {
		List<Record> records = recordService.findByAllocationID(UUID.fromString(allocationID));
		List<Identifier> identifiers = new ArrayList<>();
		for (Record record : records) {
			Identifier identifier = getDOIForRecord(record.getId().toString());
			// not all records have DOIs
			if (identifier != null) {
				identifiers.add(identifier);
			}
		}
		return identifiers;
	}

	/**
	 * Creates a new Version (if different from current one in the Registry)
	 * @param identifier the DOI
	 * @param content the Datacite XML (or error message from response)
	 * @param schemaId 'datacite-xml'
	 * @param request 'the request Object that initiated the harvest
	 * @param success ''true' if the DOI metadata is successfully harvested
	 */
	public void updateDataCiteVersion(Identifier identifier, String content, String schemaId, Request request,
			boolean success) {
		Record record = identifier.getRecord();
		if (record == null) {
			throw new ForbiddenOperationException(
					String.format("Record with Identifier %s doesn't exist", identifier.getValue()));
		}
		UUID creatorId = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		Version currentVersion = drvsVersionService.findVersionForRecord(record, schemaId);
		if (currentVersion != null) {
			String versionHash = currentVersion.getHash();
			String incomingHash = VersionService.getHash(content);
			if (incomingHash.equals(versionHash)) {
				if (success) {
					// only increment the counter if it was a successful harvest
					request.incrementAttributeValue(Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED);
				}
				return;
			}
			else {
				drvsVersionService.end(currentVersion, creatorId, request.getCreatedAt());
			}
		}

		// create new version
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schemaId);
		version.setContent(content.getBytes());
		version.setCreatorID(creatorId);
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		version.setRequestID(request.getId());
		drvsVersionService.save(version);
		logger.debug("Created a version {}", version.getId());
		if (success) {
			// only increment the counter if it was a successful harvest
			request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_CREATED);
		}
	}

	/**
	 * Create summary log message and set the status of the Request to be COMPLETED
	 * @param request the Request object
	 */
	public void finalizeRequest(Request request) {
		org.apache.logging.log4j.core.Logger requestLogger = requestService.getLoggerFor(request);
		request.setStatus(Request.Status.COMPLETED);

		int received = new Integer(request.getAttribute(Attribute.NUM_OF_RECORDS_RECEIVED));
		int errored = new Integer(request.getAttribute(Attribute.NUM_OF_ERROR));
		int total = received - errored;
		request.setMessage(String.format("Harvest completed, harvested %d record(s)", total));

		requestLogger.info("Request completed");
		save(request);
	}

	/**
	 * Use the Request logger to write logs to the Request's folder
	 * @param request the Request Object
	 * @return the Logger
	 */
	public org.apache.logging.log4j.core.Logger getLoggerFor(Request request) {
		return requestService.getLoggerFor(request);
	}

	public String getDataciteApiUrl() {
		return dataciteApiUrl;
	}

}
