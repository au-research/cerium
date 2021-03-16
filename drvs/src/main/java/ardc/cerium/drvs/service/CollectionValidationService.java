package ardc.cerium.drvs.service;

import ardc.cerium.drvs.provider.DOIMetadataQualityProvider;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.service.VocabService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.drvs.enabled")
public class CollectionValidationService {

	public static final String DRVS_CollVS = "drvs-collection-validation-summary";
	private static final Logger logger = LoggerFactory.getLogger(CollectionValidationService.class);

	final DRVSVersionService drvsVersionService;

	final VocabService vocabService;

	private final RecordService recordService;

	DOIMetadataQualityProvider doiMetadataQualityProvider;

	public CollectionValidationService(RecordService recordService, DRVSVersionService drvsVersionService,
			VocabService vocabService) {
		this.recordService = recordService;
		this.drvsVersionService = drvsVersionService;
		this.vocabService = vocabService;
		this.doiMetadataQualityProvider = new DOIMetadataQualityProvider();
		this.doiMetadataQualityProvider.setVocabService(vocabService);
	}

	/**
	 *
	 * Find the current DataCite xml version of a given record
	 * @param recordID the uuid of the record
	 * @return the Current Version or null
	 */
	public Version getCurrentDataCiteForRecord(String recordID) {
		Record record = recordService.findById(recordID);
		return drvsVersionService.findVersionForRecord(record, DOIHarvestService.DataCiteXML);
	}

	/**
	 * Find all current DataCite xml version of a given Allocation
	 * @param allocationID the uuid of the allocation
	 * @return all current DataCite xml Version of a given given AllocationID
	 */
	public List<Version> getCurrentDataCiteVersionsAllocation(String allocationID) {
		List<Record> records = recordService.findByAllocationID(UUID.fromString(allocationID));
		List<Version> versions = new ArrayList<Version>();
		for (Record record : records) {
			Version version = drvsVersionService.findVersionForRecord(record, DOIHarvestService.DataCiteXML);
			// not all records have DataCite XML Versions
			if (version != null) {
				versions.add(version);
			}
		}
		return versions;
	}

	/**
	 * Creates a new Version (if different from current one in the Registry)
	 * @param dataciteXMLVersion the current DataCite xml Version
	 * @param request 'the request Object that initiated the harvest
	 */
	public void updateValidationVersion(Version dataciteXMLVersion, Request request) {

		Record record = dataciteXMLVersion.getRecord();
		String localIdentifierValue = null;
		List<Identifier> identifiers = record.getIdentifiers();
		for (Identifier identifier : identifiers) {
			// a Record can have only 1 DRVS Identifier
			// so safe to return the first one
			if (identifier.getType().equals(Identifier.Type.DRVS)) {
				localIdentifierValue = identifier.getValue().trim();
			}
		}

		String validationResult = doiMetadataQualityProvider.get(new String(dataciteXMLVersion.getContent()),
				localIdentifierValue);

		UUID creatorId = UUID.fromString(request.getAttribute(Attribute.CREATOR_ID));
		Version currentCollectionValidationVersion;
		Optional<Version> cVersion = record.getCurrentVersions().stream()
				.filter(version -> version.getSchema().equals(DRVS_CollVS)).findFirst();
		if (cVersion.isPresent()) {
			currentCollectionValidationVersion = cVersion.get();
			String versionHash = currentCollectionValidationVersion.getHash();
			String incomingHash = VersionService.getHash(validationResult);
			if (incomingHash.equals(versionHash)) {
				return;
			}
			else {
				drvsVersionService.end(currentCollectionValidationVersion, creatorId, request.getCreatedAt());
			}
		}

		// create new version
		Version version = new Version();
		version.setRecord(record);
		version.setSchema(DRVS_CollVS);
		version.setContent(validationResult.getBytes());
		version.setCreatorID(creatorId);
		version.setCreatedAt(request.getCreatedAt());
		version.setCurrent(true);
		version.setHash(VersionService.getHash(validationResult));
		version.setRequestID(request.getId());
		drvsVersionService.save(version);
		logger.debug("Created a Collection Validation version {}", version.getId());
	}

}
