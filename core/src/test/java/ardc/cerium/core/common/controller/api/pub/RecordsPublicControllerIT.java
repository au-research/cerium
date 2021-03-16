package ardc.cerium.core.common.controller.api.pub;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.WebIntegrationTest;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.SchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class RecordsPublicControllerIT extends WebIntegrationTest {

	private final String baseUrl = "/api/public/records/";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@Test
	void index_show_shouldReturnAllRecords() {
		// 5 public records
		for (int i = 0; i < 5; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(true);
			recordRepository.save(record);
		}
		recordRepository.flush();

		this.webTestClient.get().uri(baseUrl).exchange().expectStatus().isOk().expectBody().jsonPath("$.content")
				.exists().jsonPath("$.content[*].id").isArray();
	}

	@Test
	void index_page0size5_returnsTheFirst5() {
		// 5 public records
		for (int i = 0; i < 5; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(true);
			recordRepository.save(record);
		}
		recordRepository.flush();

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("page", "0").queryParam("size", "5").build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(5)
				.jsonPath("$.content[0].id").isNotEmpty();
	}

	@Test
	void index_filterByType_returnsResults() {
		// given a Record
		recordRepository.saveAndFlush(TestHelper.mockRecord());

		// and an IGSNRecord
		Record igsnRecord = TestHelper.mockRecord();
		igsnRecord.setType("IGSN");
		recordRepository.saveAndFlush(igsnRecord);

		// @formatter:off
		this.webTestClient.get()
				.uri(uriBuilder
						-> uriBuilder.path(baseUrl)
						.queryParam("type", "IGSN")
						.build())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].id").isNotEmpty();
		// @formatter:on

	}

	@Test
	void show_notFoundOrPrivate_404() {
		// random record returns 404
		this.webTestClient.get().uri(baseUrl + UUID.randomUUID().toString()).exchange().expectStatus().isNotFound();

		// given a private record
		Record record = TestHelper.mockRecord();
		record.setVisible(false);
		recordRepository.saveAndFlush(record);

		// private record returns 403
		this.webTestClient.get().uri(baseUrl + record.getId().toString()).exchange().expectStatus().isForbidden();
	}

	@Test
	void show_publicRecord_returnsDTO() {
		// given a public record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);

		this.webTestClient.get().uri(baseUrl + record.getId().toString()).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.id").isEqualTo(record.getId().toString());
	}

	@Test
	void showVersions_publicRecord_returnsListOfVersions() {
		// given a public record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);

		// and 3 versions
		for (int i = 0; i < 3; i++) {
			Version version = TestHelper.mockVersion(record);
			versionRepository.saveAndFlush(version);
		}

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + record.getId().toString() + "/versions")
						.queryParam("page", "0").queryParam("size", "5").build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(3)
				.jsonPath("$.content[*].id").isNotEmpty();
	}

	@Test
	void showVersions_filterBySchema_returnTheRightSet() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);

		// with a version of schema ardc.cerium.core.igsn-descriptive-v1
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		version.setSchema(SchemaService.IGSNDESCv1);
		versionRepository.saveAndFlush(version);

		// and another version of schema ardc.cerium.core.igsn-csiro-v3
		Version version2 = TestHelper.mockVersion(record);
		version2.setCurrent(true);
		version2.setSchema(SchemaService.CSIROv3);
		versionRepository.saveAndFlush(version2);

		// when filter by ?schema=ardc.cerium.core.igsn-descriptive-v1, only 1 returns
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + record.getId().toString() + "/versions")
						.queryParam("schema", SchemaService.IGSNDESCv1).build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].schema").isEqualTo(SchemaService.IGSNDESCv1);
	}

}