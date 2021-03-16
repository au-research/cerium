package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.*;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.*;
import ardc.cerium.core.common.service.*;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.*;
import ardc.cerium.igsn.service.IGSNRequestService;
import ardc.cerium.igsn.service.IGSNService;
import ardc.cerium.igsn.service.IGSNVersionService;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class ImportService {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ImportService.class);

	private final IdentifierService identifierService;

	private final RecordService recordService;

	private final IGSNVersionService igsnVersionService;

	private final SchemaService schemaService;

	private final IGSNRequestService igsnRequestService;

	private final EmbargoService embargoService;

	private final long maxFileSize = 60 * 1024;

	public ImportService(IdentifierService identifierService, RecordService recordService,
						 IGSNVersionService igsnVersionService, SchemaService schemaService,
						 IGSNRequestService igsnRequestService, EmbargoService embargoService) {
		this.identifierService = identifierService;
		this.recordService = recordService;
		this.igsnVersionService = igsnVersionService;
		this.schemaService = schemaService;
		this.igsnRequestService = igsnRequestService;
		this.embargoService = embargoService;
	}

	/**
	 * Import (Ingest) a File for a Request
	 * @param file the {@link File} points to the payload or the chunked payload
	 * @param request the {@link Request} where additional details will be extracted from
	 * @return the {@link Identifier}
	 * @throws IOException when failing to read file or any other operation
	 */
	public Identifier importRequest(File file, Request request) throws IOException, ForbiddenOperationException , ContentNotSupportedException {
		Logger requestLog = igsnRequestService.getLoggerFor(request);


		// IGSN-217 each resource should be less than 64Kb (BLOB) in DB
		Helpers.checkFileSize(file.getAbsolutePath(), maxFileSize);

		// read the content of the item Resource
		String content = Helpers.readFile(file);

		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		String ownerType = request.getAttribute(Attribute.OWNER_TYPE);
		//!= null ? request.getAttribute(Attribute.OWNER_TYPE) : "User";
		String ownerID = request.getAttribute(Attribute.OWNER_ID);
		String prefix = request.getAttribute(Attribute.ALLOCATION_PREFIX);
		//!= null ? request.getAttribute(Attribute.OWNER_ID) : creatorID;

		// build the providers
		Schema schema = schemaService.getSchemaForContent(content);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		identifierProvider.setPrefix(prefix);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);
		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(schema,
				Metadata.LandingPage);
		EmbargoEndProvider embargoEnd = (EmbargoEndProvider) MetadataProviderFactory.create(schema,
				Metadata.EmbargoEnd);
		Date currentDate = new Date();



		// obtain the necessary information from the providers
		String identifierValue = identifierProvider.get(content);
		Date embargoDate = embargoEnd.get(content);

		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		// if the request is being re-played don't create mew record, url identifier and
		// version
		// but do test to make sure they are all created and contains the correct value
		if (identifier != null) {

			if (!identifier.getRequestID().equals(request.getId())) {
				logger.error("Identifier: {} already exists", identifierValue);
				throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s does exist",
						identifierValue, Identifier.Type.IGSN));
			}
			// if the Identifier was created by the same request
			// it means a record already exists
			// then this is a request that was restarted manually
			// continue to update instead and try to
			requestLog.info("Identifier: {} already exists attempting to refresh content", identifierValue);
			return updateRequest(file, request);
		}

		String landingPage = landingPageProvider.get(content);

		requestLog.debug("Ingesting Identifier: {} with Landing Page: {}", identifierValue, landingPage);

		// add new Record, Identifier, URL and Version
		Record record = new Record();
		record.setCreatedAt(request.getCreatedAt());
		record.setModifiedAt(request.getCreatedAt());
		record.setOwnerID(UUID.fromString(ownerID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(visibilityProvider.get(content));
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		record.setRequestID(request.getId());
		record.setType(IGSNService.IGSNRecordType);
		recordService.save(record);
		requestLog.debug("Added Record: {}", record.getId());

		identifier = new Identifier();
		identifier.setCreatedAt(request.getCreatedAt());
		identifier.setRecord(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		identifier.setRequestID(request.getId());
		identifier.setStatus(Identifier.Status.PENDING);
		try {
			identifierService.save(identifier);
		}
		catch (Exception e) {
			requestLog.error("Failed creating Identifier: {}", identifierValue);
			requestLog.error("Deleting created record: {}", record.getId());
			logger.error(String.format("Failed creating Identifier: %s", identifierValue));
			recordService.delete(record);
			return null;
		}
		requestLog.debug("Added Identifier: {}", identifier.getId());

		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(content));
		version.setRequestID(request.getId());
		igsnVersionService.save(version);
		requestLog.debug("Added version: {}", version.getId());


		if(embargoDate != null && embargoDate.after(currentDate) ){
			Embargo embargo = new Embargo();
			embargo.setRecord(record);
			embargo.setEmbargoEnd(embargoDate);
			embargoService.save(embargo);
			requestLog.debug("Added embargo: {} {}", embargo.getId(),embargoDate);
		}else if (embargoDate != null && embargoDate.after(currentDate)){
			record.setVisible(true);
			recordService.save(record);
		}

		// append identifierValue to the outputFile path for use in next step
		requestLog.info("Ingested {}", identifierValue);

		return identifier;
	}

	/**
	 * Update an existing IGSN Record. The workflow is different from
	 * {@link #importRequest(File, Request)}
	 * @param file the {@link File} that contains the new updated version
	 * @param request the {@link Request} that contains all additional parameters
	 * @return the IGSN {@link Identifier} that is updated
	 * @throws IOException when reading the file
	 * @throws VersionContentAlreadyExistsException when the exact same version is updated
	 */
	public Identifier updateRequest(@NotNull File file, Request request)
			throws IOException, ContentNotSupportedException, ForbiddenOperationException, VersionIsOlderThanCurrentException {
		logger.debug("Updating content for request:{} with file:{}", request, file.getAbsolutePath());
		// IGSN-217 each resource should be less than 64Kb (BLOB) in DB
		Helpers.checkFileSize(file.getAbsolutePath(), maxFileSize);
		// read the content of the item Resource
		String content = Helpers.readFile(file);
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		Schema schema = schemaService.getSchemaForContent(content);
		String prefix = request.getAttribute(Attribute.ALLOCATION_PREFIX);
		IdentifierProvider identifierProvider = (IdentifierProvider) MetadataProviderFactory.create(schema,
				Metadata.Identifier);
		identifierProvider.setPrefix(prefix);
		String identifierValue = identifierProvider.get(content);
		VisibilityProvider visibilityProvider = (VisibilityProvider) MetadataProviderFactory.create(schema,
				Metadata.Visibility);
		EmbargoEndProvider embargoEnd = (EmbargoEndProvider) MetadataProviderFactory.create(schema,
				Metadata.EmbargoEnd);
		Date currentDate = new Date();

		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		if (identifier == null) {
			throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}

		Record record = identifier.getRecord();
		if (record == null) {
			throw new ForbiddenOperationException(
					String.format("Record with Identifier %s doesn't exist", identifierValue));
		}
		// if owner type is user the creator id must be the same as the ownerid
		if (record.getOwnerType().equals(Record.OwnerType.User) && !record.getOwnerID().equals(UUID.fromString(creatorID))) {
			throw new ForbiddenOperationException(String.format("User has no access to the Record with Identifier: %s", identifierValue));
		}

		Version currentVersion = null;
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(schema.getId())).findFirst();
		if (cVersion.isPresent()) {
			currentVersion = cVersion.get();
			String versionHash = currentVersion.getHash();
			String incomingHash = VersionService.getHash(content);
			if (incomingHash.equals(versionHash) && !currentVersion.getRequestID().equals(request.getId())) {
				logger.warn("Previous version already contain the same content. Skipping");
				throw new VersionContentAlreadyExistsException(identifierValue, currentVersion.getSchema());
			}
			if(incomingHash.equals(versionHash) && currentVersion.getRequestID().equals(request.getId())){
				logger.info("Restarted Request but version already exists. Skipping");
				return identifier;
			}
		}

		// update the record
		record.setVisible(visibilityProvider.get(content));
		record.setModifierID(UUID.fromString(creatorID));
		record.setModifiedAt(request.getCreatedAt());
		recordService.save(record);
		logger.debug("Updated record {}", record.getId());

		// end current version for the given schema if it was created before this version
		boolean isThisCurrent = true;

		if (currentVersion != null && currentVersion.getCreatedAt().after(request.getCreatedAt())) {
			logger.debug(
					"Given version content is older than current version for "
							+ "Identifier {} current Date: {}, Incoming Date : {}",
					identifierValue, currentVersion.getCreatedAt(), request.getCreatedAt());
			isThisCurrent = false;
		}
		else if (currentVersion != null) {
			igsnVersionService.end(currentVersion, UUID.fromString(creatorID), request.getCreatedAt());
		}

		// create new version even if it's not the current on
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(schema.getId());
		version.setContent(content.getBytes());
		version.setCreatorID(UUID.fromString(creatorID));
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(isThisCurrent);
		version.setHash(VersionService.getHash(content));
		version.setRequestID(request.getId());
		igsnVersionService.save(version);
		logger.debug("Created a version {}", version.getId());

		if (!isThisCurrent) {
			// if not the current version don't return the Identifier to avoid
			// registration metadata being updated
			throw new VersionIsOlderThanCurrentException(identifierValue, currentVersion.getCreatedAt(),
					request.getCreatedAt());
		}

		Date embargoDate= embargoEnd.get(content);
		Embargo embargo = embargoService.findByRecord(record);
		if(embargoDate != null && embargoDate.after(currentDate)){
			//see if an embargo exists for this record
			if(embargo != null){
				embargo.setEmbargoEnd(embargoDate);
			}else{
				embargo = new Embargo();
				embargo.setRecord(record);
				embargo.setEmbargoEnd(embargoDate);
			}
			embargoService.save(embargo);
		}else if(embargoDate != null && embargoDate.before(currentDate)){
			//if an embargo exists for this record, but new embargo date is in the past
			if(embargo != null){
				embargoService.delete(embargo.getId());
			}
			record.setVisible(true);
			recordService.save(record);
		}
		if(embargoDate == null && embargo != null){
			embargoService.delete(embargo.getId());
		}

		logger.info("Updated identifier {} with a new version", identifier.getValue());
		return identifier;
	}

	public Identifier reserveRequest(String identifierValue, @NotNull Request request) {
		String creatorID = request.getAttribute(Attribute.CREATOR_ID);
		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);
		String ownerType = request.getAttribute(Attribute.OWNER_TYPE) != null
				? request.getAttribute(Attribute.OWNER_TYPE) : "User";

		// validate existing User
		Identifier existingIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (existingIdentifier != null) {
			throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s already exist",
					identifierValue, Identifier.Type.IGSN));
		}

		// create and persist new Identifier with status=RESERVED
		Record record = new Record();
		record.setCreatedAt(request.getCreatedAt());
		record.setModifiedAt(request.getCreatedAt());
		record.setOwnerID(UUID.fromString(creatorID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setVisible(false);
		record.setAllocationID(UUID.fromString(allocationID));
		record.setCreatorID(UUID.fromString(creatorID));
		record.setRequestID(request.getId());
		record.setType(IGSNService.IGSNRecordType);
		recordService.save(record);
		logger.debug("Added Record: {}", record.getId());

		Identifier identifier = new Identifier();
		identifier.setValue(identifierValue);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setCreatedAt(request.getCreatedAt());
		identifier.setRequestID(request.getId());
		identifier.setStatus(Identifier.Status.RESERVED);
		identifier.setRecord(record);

		try {
			identifierService.save(identifier);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Failed creating Identifier: {}", identifierValue);
			logger.error("Deleting created record: {}", record.getId());
			recordService.delete(record);
			return null;
		}

		//logger.info("Reserved identifier: {}", identifier.getValue());
		return identifier;
	}

	public Identifier transferRequest(String identifierValue, @NotNull Request request) {
		String ownerID = request.getAttribute(Attribute.OWNER_ID);
		String ownerType = request.getAttribute(Attribute.OWNER_TYPE);

		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		if (identifier == null) {
			throw new ForbiddenOperationException(String.format("Identifier : %s and type %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}

		Record record = identifier.getRecord();
		if(record == null){
			throw new ForbiddenOperationException(String.format("Record with Identifier: %s and type: %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}

		if(record.getOwnerID().equals(UUID.fromString(ownerID))){
			throw new NotChangedException("Record", "OwnerID", ownerID);
		}
		record.setOwnerID(UUID.fromString(ownerID));
		record.setOwnerType(Record.OwnerType.valueOf(ownerType));
		record.setModifiedAt(request.getCreatedAt());
		record.setModifierID(UUID.fromString(request.getAttribute(Attribute.CREATOR_ID)));
		recordService.save(record);

		logger.info("Transfered ownership of identifier {} to {}:{}", identifier.getValue(), ownerType, ownerID);
		return identifier;
	}

}
