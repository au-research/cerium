package ardc.cerium.drvs.service;

import ardc.cerium.drvs.TestHelper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.drvs.model.DRVSSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@TestPropertySource(properties = { "app.drvs.enabled=true", "spring.jpa.open-in-view=false",
		"spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true" })
public class DRVSImportServiceIT {

	@Autowired
	DRVSImportService drvsImportService;

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	VersionRepository versionRepository;

	@Test
	@DisplayName("Ingesting a new DRVS Submission results in proper correct content")
	void ingest() throws Exception {
		// given a request
		Request request = TestHelper.mockRequest();
		UUID creatorID = UUID.randomUUID();
		UUID ownerID = UUID.randomUUID();
		UUID allocationID = UUID.randomUUID();
		request.setAttribute(Attribute.CREATOR_ID, creatorID.toString());
		request.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString());
		request.setAttribute(Attribute.OWNER_ID, ownerID.toString());
		request.setAttribute(Attribute.OWNER_TYPE, Record.OwnerType.DataCenter.toString());

		// and a drvs submission
		DRVSSubmission submission = new DRVSSubmission();
		submission.setLocalCollectionID(UUID.randomUUID().toString());
		submission.setTitle("Test Title");

		// when ingest
		drvsImportService.ingest(submission, request);

		// see a new record is created with the request id
		List<Record> results = recordRepository.findByCreatorID(creatorID);
		assertThat(results).hasSize(1);

		// identifier is created with the correct fields
		Identifier identifier = identifierRepository.findByValueAndTypeAndDomain(submission.getLocalCollectionID(),
				Identifier.Type.DRVS, allocationID.toString());
		assertThat(identifier.getValue()).isEqualTo(submission.getLocalCollectionID());
		assertThat(identifier.getType()).isEqualTo(Identifier.Type.DRVS);
		assertThat(identifier.getDomain()).isEqualTo(allocationID.toString());

		// record is created with the correct fields
		Record record = identifier.getRecord();
		assertThat(record).isNotNull();
		assertThat(record).isInstanceOf(Record.class);
		assertThat(record.getType()).isEqualTo(DRVSImportService.DRVS_RECORD_TYPE);
		assertThat(record.getTitle()).isEqualTo(submission.getTitle());
		assertThat(record.getOwnerID()).isEqualTo(ownerID);
		assertThat(record.getAllocationID()).isEqualTo(allocationID);

		// version is created with the correct data
		List<Version> versions = versionRepository.findAllByRecordAndSchemaAndCurrentIsTrue(record,
				DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID);
		assertThat(versions).hasSize(1);
		Version version = versions.get(0);
		String content = new String(version.getContent());
		assertThat(content).isNotNull();
	}

	@Test
	@DisplayName("Ingesting an existing record should update it")
	void ingest_update() throws Exception {
		UUID creatorID = UUID.randomUUID();
		UUID allocationID = UUID.randomUUID();
		UUID localCollectionID = UUID.randomUUID();

		// prepare the request
		Request updateRequest = TestHelper.mockRequest();
		updateRequest.setAttribute(Attribute.CREATOR_ID, creatorID.toString());
		updateRequest.setAttribute(Attribute.ALLOCATION_ID, allocationID.toString());
		DRVSSubmission updateSubmission = new DRVSSubmission();
		updateSubmission.setLocalCollectionID(localCollectionID.toString());
		updateSubmission.setTitle("Updated Title");

		// given an existing record
		Record record = TestHelper.mockRecord();
		record.setTitle("Test Title");
		recordRepository.saveAndFlush(record);

		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue(localCollectionID.toString());
		identifier.setType(Identifier.Type.DRVS);
		identifier.setDomain(allocationID.toString());
		identifierRepository.saveAndFlush(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		version.setSchema(DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID);
		version.setHash("blah");
		versionRepository.saveAndFlush(version);

		// when ingest
		drvsImportService.ingest(updateSubmission, updateRequest);

		// should update it instead of creating a new one,
		// new version is created, updated title
		Identifier actualIdentifier = identifierRepository.findByValueAndTypeAndDomain(localCollectionID.toString(),
				Identifier.Type.DRVS, allocationID.toString());
		assertThat(actualIdentifier).isNotNull();

		Record actualRecord = actualIdentifier.getRecord();
		assertThat(actualRecord).isNotNull();
		assertThat(actualRecord.getTitle()).isEqualTo("Updated Title");

		// there are now 2 drvs-submission for that record, with 1 being the current
		Set<Version> versions = actualRecord.getVersions().stream()
				.filter(version1 -> version1.getSchema().equals(DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID))
				.collect(Collectors.toSet());
		assertThat(versions).hasSize(2);

		// with 1 being the current
		Version latestDRVSSubmissionVersion = actualRecord.getCurrentVersions().stream()
				.filter(version1 -> version1.getSchema().equals(DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID))
				.findFirst().orElse(null);
		assertThat(latestDRVSSubmissionVersion).isNotNull();
	}

}
