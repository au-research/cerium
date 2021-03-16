package ardc.cerium.drvs.service;

import ardc.cerium.drvs.model.DRVSSubmission;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.VersionContentAlreadyExistsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.drvs.enabled")
public class DRVSImportService {

	public static final String DRVS_RECORD_TYPE = "DRVS";

	public static final String DRVS_SUBMISSION_SCHEMA_ID = "drvs-submission";

	private static final String DOI_DOMAIN = "datacite.org";

	private static final String doiUrlComponenet = "://doi.org/";

	private static final Logger logger = LoggerFactory.getLogger(DRVSImportService.class);

	private final RequestService requestService;

	private final RecordService recordService;

	private final DRVSVersionService drvsVersionService;

	private final IdentifierService identifierService;

	public DRVSImportService(RequestService requestService, RecordService recordService,
			DRVSVersionService drvsVersionService, IdentifierService identifierService) {
		this.requestService = requestService;
		this.recordService = recordService;
		this.drvsVersionService = drvsVersionService;
		this.identifierService = identifierService;
	}

	/**
	 * Persist the {@link Request}
	 * @param request The {@link Request} to persist
	 * @return the persisted {@link Request}
	 */
	public Request save(Request request) {
		return requestService.save(request);
	}

	/**
	 * Create a new Import Request for DRVS service
	 * @param user The {@link User} that originated the request
	 * @param file The uploaded {@link MultipartFile} Posted along with the request
	 * @return The persisted {@link Request}
	 */
	public Request createRequest(User user, MultipartFile file) {
		Request request = new Request();
		request.setType("drvs-import");
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
		String payloadPath = dataPath + File.separator + file.getOriginalFilename();
		try {
			file.transferTo(new File(payloadPath));
			request.setAttribute(Attribute.PAYLOAD_PATH, payloadPath);
		}
		catch (IOException e) {
			logger.error("Failed to upload file, reason: {}", e.getMessage());
		}

		request = save(request);
		return request;
	}

	/**
	 * Ingest and persist a {@link DRVSSubmission}. Creating the {@link Record} and
	 * {@link Version}
	 * @param submission the {@link DRVSSubmission} to ingest
	 * @param request the {@link Request} that the submission comes from
	 * @return record the successfully ingested {@link Record}
	 * @throws Exception when there's a problem ingesting the submission
	 */
	public Record ingest(DRVSSubmission submission, Request request) throws Exception {
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		String localIdentifier = submission.getLocalCollectionID();
		Record record = null;

		org.apache.logging.log4j.core.Logger requestLogger = requestService.getLoggerFor(request);

		if (localIdentifier == null || localIdentifier.trim().isEmpty()) {
			request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
			requestLogger.error("Encountered entry without local identifier");
			throw new Exception("Encountered entry without local identifier");
		}

		Identifier existingIdentifier = identifierService.findByValueAndTypeAndDomain(localIdentifier,
				Identifier.Type.DRVS, allocationID);

		if (existingIdentifier == null) {
			requestLogger.debug("No existing identifier with value {} and domain {} found, creating new record",
					localIdentifier, allocationID);

			try {
				record = createNewDRVSRecord(submission, request);
				logger.info("Created new record for DRVS: {}", localIdentifier);
				request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_CREATED);
				requestLogger.info("Created new DRVS Record for local identifier ID: {}", localIdentifier);
			}
			catch (JsonProcessingException ex) {
				request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
				requestLogger.error("Encountered data issue for local identifier: {}", localIdentifier);
				logger.error("Encountered data issue for local identifier: {} Exception: {}", localIdentifier,
						ex.getMessage());
			}
			catch (Exception e) {
				request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
				requestLogger.error(e.getMessage());
				logger.error("Encountered unexpected issue for localIdentifier: {} Exception: {}", localIdentifier,
						e.getMessage());
			}
		}
		else {
			requestLogger.debug("Found existing identifier {}, updating existing record", localIdentifier);
			logger.info("Updating DRVS: {}, found existing record", localIdentifier);
			try {
				record = modifyExistingDRVSRecord(existingIdentifier, submission, request);
				request.incrementAttributeValue(Attribute.NUM_OF_RECORDS_UPDATED);
				requestLogger.info("Updated Record for local identifier: {}", localIdentifier);
			}
			catch (JsonProcessingException ex) {
				request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
				requestLogger.error("Encountered data issue for local identifier: {}", localIdentifier);
				logger.error("Encountered data issue for record: {}, Reason: {}", localIdentifier, ex.getMessage());
			}
			catch (VersionContentAlreadyExistsException ex) {
				request.incrementAttributeValue(Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED);
				requestLogger.info("Version content for record {} is unchanged, no action taken", localIdentifier);
				logger.info("Version content for record: {} is unchanged, no action taken", localIdentifier);
			}
			catch (ForbiddenOperationException e) {
				request.incrementAttributeValue(Attribute.NUM_OF_ERROR);
				requestLogger.error("Encountered data issue for local identifier: {}, Exception: {}", localIdentifier,
						e.getMessage());
				logger.error("Encountered data issue for record: {}, Reason: {}", localIdentifier, e.getMessage());
			}
		}
		save(request);
		return record;
	}

	/**
	 * @param submission the {@link DRVSSubmission} to create record/identifier/versions
	 * on
	 * @param request the {@link Request} that the submission came in
	 * @return record the successfully created {@link Record}
	 * @throws JsonProcessingException when serializing the submission json failed
	 * @throws Exception for general exceptions
	 */
	private Record createNewDRVSRecord(DRVSSubmission submission, Request request)
			throws JsonProcessingException, Exception {
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		String ownerID = request.getAttribute(Attribute.OWNER_ID);
		String ownerType = request.getAttribute(Attribute.OWNER_TYPE);

		// creating the record
		Record record = new Record();
		Version version = new Version();
		Identifier identifier = new Identifier();
		Identifier newDOI = new Identifier();

		try {
			record.setRequestID(request.getId());
			record.setCreatedAt(new Date());
			record.setModifiedAt(new Date());
			record.setCreatorID(UUID.fromString(creatorID));
			record.setOwnerID(UUID.fromString(ownerID));
			record.setOwnerType(Record.OwnerType.valueOf(ownerType));
			record.setModifierID(UUID.fromString(creatorID));
			record.setType(DRVS_RECORD_TYPE);
			record.setAllocationID(UUID.fromString(allocationID));
			record.setTitle(submission.getTitle());
			recordService.save(record);
			logger.info("Created new record {}", record.getId());
		}
		catch (Exception e) {
			logger.error("Failed to create record. Reason: {}", e.getMessage());
		}

		// creating the identifier

		// this could fail due to race condition

		try {
			identifier.setRecord(record);
			identifier.setType(Identifier.Type.DRVS);
			identifier.setValue(submission.getLocalCollectionID());
			identifier.setDomain(allocationID);
			identifier.setRequestID(request.getId());
			identifier.setStatus(Identifier.Status.ACCESSIBLE);
			identifier.setCreatedAt(new Date());
			identifier.setUpdatedAt(new Date());
			identifier.setRequestID(request.getId());
			identifierService.save(identifier);
			logger.info("Created new Identifier {}", identifier.getId());
		}
		catch (Exception e) {
			// if it fails, remove the record that was created
			recordService.delete(record);
			throw new Exception(String.format("Failed to insert Identifier %s due to %s",
					submission.getLocalCollectionID(), e.getMessage()));
		}

		// if there's a DOI available, create the DOI identifier

		if (submission.getDOI() != null && !submission.getDOI().trim().isEmpty()) {
			try {

				String identifierValue = submission.getDOI().trim();
				if(identifierValue.contains(doiUrlComponenet))
				{
					identifierValue = identifierValue.substring(identifierValue.indexOf(doiUrlComponenet) + doiUrlComponenet.length());
				}
				newDOI.setRecord(record);
				newDOI.setType(Identifier.Type.DOI);
				newDOI.setValue(identifierValue);
				newDOI.setDomain(DOI_DOMAIN);
				newDOI.setRequestID(request.getId());
				newDOI.setStatus(Identifier.Status.ACCESSIBLE);
				newDOI.setCreatedAt(new Date());
				newDOI.setUpdatedAt(new Date());
				newDOI.setRequestID(request.getId());
				identifierService.save(newDOI);
			}
			catch (Exception e) {
				identifierService.delete(identifier.getId().toString());
				recordService.delete(record);
				throw new Exception(String.format("Failed to insert DOI Identifier for DRVS record:%s due to %s",
						submission.getLocalCollectionID(), e.getMessage()));
			}
		}

		// create version
		// write drvs-submission version as json string
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(submission);
		try {
			version.setRecord(record);
			version.setSchema(DRVS_SUBMISSION_SCHEMA_ID);
			version.setContent(json.getBytes());
			version.setHash(VersionService.getHash(json));
			version.setCurrent(true);
			version.setRequestID(request.getId());
			version.setCreatorID(UUID.fromString(creatorID));
			drvsVersionService.save(version);
			logger.info("Created new Version {}", version.getId());
		}
		catch (Exception e) {
			identifierService.delete(identifier.getId().toString());
			identifierService.delete(newDOI.getId().toString());
			recordService.delete(record);
			logger.error("Failed to create the DRVS Version. Reason: {}", e.getMessage());
			logger.info("Deleting local and DOI Identifiers and Record");
			throw new Exception(String.format("Failed to insert Version for DRVS record:%s due to %s",
					submission.getLocalCollectionID(), e.getMessage()));
		}

		return record;

	}

	/**
	 * @param identifier The DRVS {@link Identifier} to be used to update
	 * @param submission The DRVS {@link DRVSSubmission} to update on
	 * @param request the Update {@link Request}
	 * @return record the successfully updated {@link Record}
	 * @throws JsonProcessingException when failing to serialize the submission json
	 * @throws VersionContentAlreadyExistsException when the version content is identical
	 */
	private Record modifyExistingDRVSRecord(Identifier identifier, DRVSSubmission submission, Request request)
			throws JsonProcessingException, VersionContentAlreadyExistsException, ForbiddenOperationException {
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		boolean versionUpdated = false;
		boolean doiUpdated = false;
		Record record = identifier.getRecord();
		// todo check record == null

		Version latestDRVSSubmissionVersion = drvsVersionService.findVersionForRecord(record,
				DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID);

		// todo check latestDRVSSubmissionVersion == null

		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(submission);

		String newHash = VersionService.getHash(json);

		// check hash to see if the data is unchanged
		Version version = null;
		if (latestDRVSSubmissionVersion != null && latestDRVSSubmissionVersion.getHash().equals(newHash)) {
			logger.info("Existing DRVS record is the same for local Identifier : {}", identifier.getValue());
		}
		else {
			versionUpdated = true;
			// create a new one and make it current
			version = new Version();
			version.setRecord(record);
			version.setSchema(DRVS_SUBMISSION_SCHEMA_ID);
			version.setContent(json.getBytes());
			version.setHash(VersionService.getHash(json));
			version.setCurrent(true);
			version.setRequestID(request.getId());
			version.setCreatorID(UUID.fromString(creatorID));
			version.setCreatedAt(request.getCreatedAt());
			logger.info("Created new version: {}", version.getId());
		}

		// Check for existing DOI
		Identifier existingDOI = record.getIdentifiers().stream()
				.filter(identifier1 -> identifier1.getType().equals(Identifier.Type.DOI)).findFirst().orElse(null);
		// if there is an existing one
		// check if it has changed!
		if (existingDOI != null) {
			try {
				// if there is a DOI in the submission check if they are the same
				String oldDOIValue = existingDOI.getValue();
				if (submission.getDOI() != null && !submission.getDOI().trim().isEmpty()) {
					String newDOIValue = submission.getDOI().trim();
					if(newDOIValue.contains(doiUrlComponenet))
					{
						newDOIValue = newDOIValue.substring(newDOIValue.indexOf(doiUrlComponenet) + doiUrlComponenet.length());
					}
					if (oldDOIValue.equals(newDOIValue)) {
						logger.info("Found existing DOI: {} and it is the same as given one no action required",
								oldDOIValue);
					}
					else {
						doiUpdated = true;
						// Update the old DOI's Identifier
						logger.info("DOI Change detected in DRVS record Identifier: {}", identifier.getValue());
						existingDOI.setValue(newDOIValue);
						existingDOI.setUpdatedAt(new Date());
						existingDOI.setRequestID(request.getId());
						identifierService.save(existingDOI);
						logger.info("Updated DOI: from {} , to {}", oldDOIValue, newDOIValue);
					}
				}
				else {// if there is an old DOI but there is no DOI in this update delete
						// the Identifier
					doiUpdated = true;
					logger.info("Deleting existing DOI: {}", existingDOI.getValue());
					record.getIdentifiers().remove(existingDOI);
					record = recordService.save(record);
					identifierService.delete(existingDOI.getId().toString());
				}
			}
			catch (Exception e) {
				logger.error("Failed to update or delete Identifier {}, Reason: {}", existingDOI.getId(),
						e.getMessage());
				throw new ForbiddenOperationException(
						String.format("Failed to update DRVS Record with Identifier %s ", identifier.getValue()));
			}
			// if the old DOI is different from the new one or there is no new one
			// end the current dataCite XML and the quality versions
			if (doiUpdated) {
				Version dcVersion = drvsVersionService.findVersionForRecord(record, DOIHarvestService.DataCiteXML);
				if (dcVersion != null) {
					drvsVersionService.end(dcVersion, UUID.fromString(creatorID), request.getCreatedAt());
					logger.info("Ended current DataCite XML for {}", identifier.getValue());
				}
				// unvalidated DataCite xml, by setting current is false if exist
				// set validation version not current is exist
				Version mqVersion = drvsVersionService.findVersionForRecord(record,
						CollectionValidationService.DRVS_CollVS);
				if (dcVersion != null) {
					logger.info("Ended current Collection Validation data for {}", identifier.getValue());
					drvsVersionService.end(mqVersion, UUID.fromString(creatorID), request.getCreatedAt());
				}
			}
		} // if there was no existing DOI Identifier check if there is one now
		else if (submission.getDOI() != null && !submission.getDOI().trim().isEmpty()) {
			logger.info("New DOI found for DRVS record : {}", identifier.getValue());
			Identifier newDOI = new Identifier();
			String newDOIValue = submission.getDOI().trim();
			if(newDOIValue.contains(doiUrlComponenet))
			{
				newDOIValue = newDOIValue.substring(newDOIValue.indexOf(doiUrlComponenet) + doiUrlComponenet.length());
			}
			try {
				newDOI.setRecord(record);
				newDOI.setType(Identifier.Type.DOI);
				newDOI.setValue(newDOIValue);
				newDOI.setDomain(DOI_DOMAIN);
				newDOI.setStatus(Identifier.Status.ACCESSIBLE);
				newDOI.setCreatedAt(new Date());
				newDOI.setUpdatedAt(new Date());
				newDOI.setRequestID(request.getId());
				record.getIdentifiers().add(newDOI);
				identifierService.save(newDOI);
				doiUpdated = true;
				logger.info("Created new DOI: {}", newDOI.getValue());
			}
			catch (Exception e) {
				throw new ForbiddenOperationException(
						String.format("Failed to update DRVS Record with Identifier %s due to %s",
								identifier.getValue(), e.getMessage()));

			}
		}

		if (versionUpdated | doiUpdated) {// update record
			if (latestDRVSSubmissionVersion != null) {
				drvsVersionService.end(latestDRVSSubmissionVersion, UUID.fromString(creatorID), request.getCreatedAt());
				logger.info("Ended previous version: {}", latestDRVSSubmissionVersion.getId());
			}
			drvsVersionService.save(version);
			record.setTitle(submission.getTitle());
			record.setModifiedAt(request.getCreatedAt());
			record.setModifierID(UUID.fromString(creatorID));
			try {
				record = recordService.save(record);
				logger.info("Updated record: {}", record.getId());
			}
			catch (Exception e) {

				logger.error("Failed to update record {}. Reason: {}", record.getId(), e.getMessage());
				throw new ForbiddenOperationException(
						String.format("Unable to update DRVS Record with Identifier %s", identifier.getValue()));
			}
		}
		else {
			throw new VersionContentAlreadyExistsException(identifier.getValue(), DRVS_SUBMISSION_SCHEMA_ID);
		}

		return record;
	}

	public void finalizeRequest(Request request) {
		org.apache.logging.log4j.core.Logger requestLogger = requestService.getLoggerFor(request);

		request.setStatus(Request.Status.COMPLETED);

		// todo give message more information
		request.setMessage("Import Completed");

		requestLogger.info("Request Completed");
	}

}
