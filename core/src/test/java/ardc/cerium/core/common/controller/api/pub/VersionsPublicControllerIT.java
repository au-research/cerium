package ardc.cerium.core.common.controller.api.pub;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.WebIntegrationTest;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

class VersionsPublicControllerIT extends WebIntegrationTest {

	private final String baseUrl = "/api/public/versions/";

	@Autowired
	VersionRepository versionRepository;

	@Autowired
	RecordRepository recordRepository;

	@BeforeEach
	void setUp() {
		versionRepository.flush();
		versionRepository.deleteAll();
		versionRepository.flush();
	}

	@Test
	void index_mixedVersions_showOnlyPublicVersions() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);

		// with a current version
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		versionRepository.saveAndFlush(version);

		// and a superseded version
		Version superseded = TestHelper.mockVersion(record);
		superseded.setCurrent(false);
		versionRepository.saveAndFlush(superseded);

		// given a record
		Record privateRecord = TestHelper.mockRecord();
		privateRecord.setVisible(false);
		recordRepository.saveAndFlush(privateRecord);

		// current version on private record
		Version privateVersion = TestHelper.mockVersion(privateRecord);
		privateVersion.setCurrent(true);
		versionRepository.saveAndFlush(privateVersion);

		// when show all, only have 1 current, public version
		this.webTestClient.get().uri(baseUrl).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.numberOfElements").isEqualTo(1);
	}

	@Test
	void index_filterBySchema() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);

		// with a version of schema ardc.cerium.core.igsn-descriptive-v1
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		version.setSchema("ardc.cerium.core.igsn-descriptive-v1");
		versionRepository.saveAndFlush(version);

		// and another version of schema ardc.cerium.core.igsn-csiro-v3
		Version version2 = TestHelper.mockVersion(record);
		version2.setCurrent(true);
		version2.setSchema("ardc.cerium.core.igsn-csiro-v3");
		versionRepository.saveAndFlush(version2);

		// both returns with no schema query
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("schema", "ardc.cerium.core.igsn-descriptive-v1").build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].schema").isEqualTo("ardc.cerium.core.igsn-descriptive-v1");

		// when filter by ?schema=ardc.cerium.core.igsn-descriptive-v1, only 1 returns
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("schema", "ardc.cerium.core.igsn-descriptive-v1").build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].schema").isEqualTo("ardc.cerium.core.igsn-descriptive-v1");

		// when filter by ?schema=ardc.cerium.core.igsn-csiro-v3, only 1 returns
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("schema", "ardc.cerium.core.igsn-csiro-v3").build()).exchange()
				.expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].schema").isEqualTo("ardc.cerium.core.igsn-csiro-v3");
	}

	@Test
	void index_filterByRecord() {
		// given a public version
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		versionRepository.saveAndFlush(version);

		// given a public version
		Record anotherRecord = TestHelper.mockRecord();
		anotherRecord.setVisible(true);
		recordRepository.saveAndFlush(anotherRecord);
		Version anotherVersion = TestHelper.mockVersion(anotherRecord);
		anotherVersion.setCurrent(true);
		versionRepository.saveAndFlush(anotherVersion);

		// when filter by ?record=record, only 1 returns
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("record", record.getId().toString()).build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1)
				.jsonPath("$.content[0].record").isEqualTo(record.getId().toString());
	}

	@Test
	void show_notfoundOrPrivate_404() {
		// given a notFound record
		this.webTestClient.get().uri(baseUrl + UUID.randomUUID().toString()).exchange().expectStatus().isNotFound();

		// given a private version
		Record record = TestHelper.mockRecord();
		record.setVisible(false);
		recordRepository.saveAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		versionRepository.saveAndFlush(version);

		this.webTestClient.get().uri(baseUrl + version.getId().toString()).exchange().expectStatus().isNotFound();
	}

	@Test
	void show_publicVersion_200() {
		// given a public version
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		versionRepository.saveAndFlush(version);

		this.webTestClient.get().uri(baseUrl + version.getId().toString()).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.id").isEqualTo(version.getId().toString()).jsonPath("$.record")
				.isEqualTo(record.getId().toString());
	}

	@Test
	void showContent_public_200XML() throws IOException {
		// given a public version
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		recordRepository.saveAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		String xml = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");
		version.setSchema(SchemaService.CSIROv3);
		version.setContent(xml.getBytes());
		version.setCurrent(true);
		versionRepository.saveAndFlush(version);

		this.webTestClient.get().uri(baseUrl + version.getId().toString() + "/content").exchange().expectStatus().isOk()
				.expectBody().xml(xml);
	}

}