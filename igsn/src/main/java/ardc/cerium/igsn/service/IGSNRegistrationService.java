package ardc.cerium.igsn.service;

import ardc.cerium.core.common.entity.*;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.LandingPageProvider;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.*;
import ardc.cerium.core.common.transform.Transformer;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.NotFoundException;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.igsn.client.MDSClient;
import ardc.cerium.igsn.model.IGSNAllocation;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;

@Service
@ConditionalOnProperty(name = "app.igsn.enabled")
public class IGSNRegistrationService {

	@Autowired
	private IdentifierService identifierService;

	@Autowired
	private IGSNVersionService igsnVersionService;

	@Autowired
	private URLService urlService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private IGSNRequestService igsnRequestService;

	@Autowired
	private KeycloakService keycloakService;

	private final List<String> supportedSchemas = Arrays.asList(SchemaService.ARDCv1, SchemaService.CSIROv3);


	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IGSNRegistrationService.class);

	/**
	 * @param identifierValue {@link Identifier}'s Value as String
	 * @param request the {@link Request} this identifier is created/updated by
	 * @throws Exception if registering or updating fails throws
	 */
	public void registerIdentifier(String identifierValue, Request request) throws Exception {
		logger.info("Registering Identifier " + identifierValue);
		Logger requestLog = igsnRequestService.getLoggerFor(request);

		String igsnMsg = "";
		String metadataMsg = "";
		if (request.getType().equals(IGSNService.EVENT_MINT)
				|| request.getType().equals(IGSNService.EVENT_BULK_MINT)) {
			igsnMsg = "minted";
			metadataMsg = "created";
		}
		else if (request.getType().equals(IGSNService.EVENT_UPDATE)
				|| request.getType().equals(IGSNService.EVENT_BULK_UPDATE)) {
			igsnMsg = "updated";
			metadataMsg = "updated";
		}


		String allocationID = request.getAttribute(Attribute.ALLOCATION_ID);

		IGSNAllocation allocation = (IGSNAllocation) keycloakService.getAllocationByResourceID(allocationID);
		allocation.setIGSNAllocationAttributes();

		// obtain the Identifier
		Identifier identifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		if (identifier == null) {
			requestLog.error("Failed to obtain Identifier : {} with Type: {}", identifierValue, Identifier.Type.IGSN);
			throw new ForbiddenOperationException(String.format("Identifier with value %s and type %s doesn't exist",
					identifierValue, Identifier.Type.IGSN));
		}

		// obtain record
		Record record = identifier.getRecord();
		if (record == null) {
			requestLog.error("Failed to obtain associated record for identifier: {}", identifier.getId());
			throw new RecordNotFoundException(identifier.getId().toString());
		}

		// obtain latest version from supported schemas
		// we need to support CSIROv3 and ARDCv1 at least !!
		Version supportedVersion = null;

		for(String supportedSchema: supportedSchemas){
			Version v = igsnVersionService.getCurrentVersionForRecord(record, supportedSchema);
			if(v != null){
				if(supportedVersion == null){
					supportedVersion = v;
				}
				else if(supportedVersion.getCreatedAt().before(v.getCreatedAt())){
					supportedVersion = v;
				}
			}
		}

		if (supportedVersion == null) {
			requestLog.error("Unable to generate registration metadata missing supported Schema version");
			throw new NotFoundException(
					"Unable to generate registration metadata missing supported Schema version");
		}

		Schema fromSchema = schemaService.getSchemaByID(supportedVersion.getSchema());


		LandingPageProvider landingPageProvider = (LandingPageProvider) MetadataProviderFactory.create(fromSchema,
				Metadata.LandingPage);

		String landingPage = landingPageProvider.get(new String(supportedVersion.getContent()));

		boolean hasLandingPageChanged = updateLandingPage(landingPage, record, request);
		// Update the URL of the IGSN at MDS
		// if the URL Changed or for some reason the previous request has failed and IGSN is still PENDING or RESERVED

		if (hasLandingPageChanged || identifier.getStatus().equals(Identifier.Status.PENDING) || identifier.getStatus().equals(Identifier.Status.RESERVED)) {
			MDSClient mdsClient = new MDSClient(allocation);
			mdsClient.createOrUpdateIdentifier(identifierValue, landingPage);
			requestLog.debug("Successfully {} Identifier {} with Landing Page {}", igsnMsg, identifierValue, landingPage);
			logger.info(String.format("Successfully %s Identifier %s with Landing Page %s", igsnMsg, identifierValue, landingPage));

		}

		// transform to registration metadata

		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);

		// obtain landing page
		logger.debug("fromSchema: {}", fromSchema.getName());
		logger.debug("toSchema: {}", toSchema.getName());

		Transformer transformer = (Transformer) TransformerFactory
				.create(fromSchema, toSchema);

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String utcDateTimeStr = df.format(supportedVersion.getCreatedAt());
		transformer.setParam("timeStamp", utcDateTimeStr).setParam("registrantName", allocation.getMds_username());
		transformer.setParam("prefix", request.getAttribute(Attribute.ALLOCATION_PREFIX));
		transformer.getParams().forEach((key, value) -> requestLog.debug("Transformer.{}: {}", key, value));

		Version registrationMetadataVersion = transformer.transform(supportedVersion);
		boolean hasRegistrationMetadataChanged = addRegistrationMetadataVersion(registrationMetadataVersion, record,
				request);
		// update the registration Metadata at MDS
		// or for some reason the previous request has failed and IGSN is still PENDING or RESERVED
		if (hasRegistrationMetadataChanged || identifier.getStatus().equals(Identifier.Status.PENDING) || identifier.getStatus().equals(Identifier.Status.RESERVED)) {
			MDSClient mdsClient = new MDSClient(allocation);
			mdsClient.addMetadata(new String(registrationMetadataVersion.getContent()));
			requestLog.debug("Successfully {} Registration Metadata for Identifier {}", metadataMsg, identifierValue);
			logger.info(String.format("Successfully %s Registration Metadata for Identifier %s", metadataMsg, identifierValue));
		}

		// if it's a single event then the request is completed
		if (request.getType().equals(IGSNService.EVENT_MINT) || request.getType().equals(IGSNService.EVENT_UPDATE)) {
			request.setStatus(Request.Status.COMPLETED);
		}



		// if the Identifier is PENDING OR RESERVED
		// we set it to be ACCESSIBLE after MINT or UPDATE request
		if (!identifier.getStatus().equals(Identifier.Status.ACCESSIBLE) &&
				(request.getType().equals(IGSNService.EVENT_MINT)
				|| request.getType().equals(IGSNService.EVENT_BULK_MINT)
				|| request.getType().equals(IGSNService.EVENT_UPDATE)
				|| request.getType().equals(IGSNService.EVENT_BULK_UPDATE))) {
			identifier.setStatus(Identifier.Status.ACCESSIBLE);
			identifierService.update(identifier);
		}


	}

	/**
	 * Helper method to add a registration Metadata version and set it to current
	 * @param version the {@link Version} to persist
	 * @param record the {@link Record} to link with
	 * @param request the {@link Request} that created the update
	 * @return boolean true if new version was added
	 */
	private boolean addRegistrationMetadataVersion(@NotNull Version version, Record record, Request request) {
		Logger requestLog = igsnRequestService.getLoggerFor(request);
		UUID creatorID = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		Version currentVersion = igsnVersionService.getCurrentVersionForRecord(record, SchemaService.IGSNREGv1);

		if (currentVersion != null) {
			// the version is later than the current request (shouldn't happen unless
			// requests are re-run
			if (currentVersion.getCreatedAt().after(request.getCreatedAt())) {
				logger.info(String.format("Current Version is newer created at: %s" ,currentVersion.getCreatedAt()));
				return false;
			}
			// the current version was created by the same request
			if (currentVersion.getRequestID() != null && currentVersion.getRequestID().equals(request.getId())) {
				logger.info(String.format("Current Version already the same created at: %s" , currentVersion.getCreatedAt()));
				return false;
			}
			// there is current version then end it now
			igsnVersionService.end(currentVersion, creatorID, request.getCreatedAt());
		}
		// if we got this far a new version will be added
		version.setRecord(record);
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setCreatorID(creatorID);
		version.setRequestID(request.getId());
		igsnVersionService.save(version);

		return true;
	}

	/**
	 * @param landingPage the url of the landing page
	 * @param record the record of the landing page
	 * @param request the request this version was provided by
	 * @return boolean (true) if the landing page is new or different
	 */
	private boolean updateLandingPage(String landingPage, Record record, Request request) {
		URL url = urlService.findByRecord(record);

		// if it's the first time this record is created
		if (url == null) {
			url = new URL();
			url.setRecord(record);
			url.setUrl(landingPage);
			url.setResolvable(true);
			url.setCreatedAt(request.getCreatedAt());
			urlService.create(url);
			return true;
		}
		// if the landingpage didn't change
		else if (url.getUrl().toLowerCase().equals(landingPage.toLowerCase())) {
			return false;
		}
		// set the url to the current landing page
		else {
			url.setUrl(landingPage);
			urlService.update(url);
			return true;
		}
	}

}
