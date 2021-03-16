package ardc.cerium.drvs.controller;

import ardc.cerium.drvs.KeycloakIntegrationTest;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.drvs.service.DRVSImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.drvs.enabled=true")
class DRVSServiceControllerIT extends KeycloakIntegrationTest {

	private final String importUrl = "/api/services/drvs/import/";

	@Autowired
	IdentifierService identifierService;

	@Autowired
	private IdentifierRepository identifierRepository;

	@Test
	@DisplayName("Upload a csv with a single submission should result in a single record, version and identifier created")
	void upload_csv_happy_path() {

		// given a multipart upload
		MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
		multipartData.add("file", new ClassPathResource("data/drvs.csv"));
		multipartData.add("allocationID", resourceID);

		// when post to the import endpoint, should be 200
		this.webTestClient.post().uri(importUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(BodyInserters.fromMultipartData(multipartData)).exchange().expectStatus().isOk();

		// there is now an Identifier DRVS al33 created
		Identifier identifier = identifierRepository.findByValueAndTypeAndDomain("al33", Identifier.Type.DRVS,
				resourceID);
		assertThat(identifier).isNotNull();

		// that has a record
		Record record = identifier.getRecord();
		assertThat(record).isNotNull();
		assertThat(record.getTitle()).isEqualTo("Earth System Grid Federation (ESGF) Replicated CMIP5-era Datasets");
		assertThat(record.getType()).isEqualTo(DRVSImportService.DRVS_RECORD_TYPE);
		assertThat(record.getOwnerType()).isEqualTo(Record.OwnerType.DataCenter);
		// assertThat(record.getOwnerID()).isEqualTo(resourceID);

		// the record has a current drvs-submission version
		List<Version> versions = record.getCurrentVersions();
		List<Version> drvsSubmissionVersions = versions.stream()
				.filter((version) -> version.getSchema().equals(DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID))
				.collect(Collectors.toList());
		assertThat(drvsSubmissionVersions).hasSize(1);
	}

}