package ardc.cerium.igsn.task;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.igsn.WebIntegrationTest;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class TransformJSONLDTaskIT extends WebIntegrationTest {

	@Autowired
	VersionService versionService;

	@Autowired
	RecordService recordService;

	@Autowired
	SchemaService schemaService;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private VersionRepository versionRepository;

	@Test
	void run() throws Exception {

		// given a record with a current version with schema ardcv1
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		Version version = TestHelper.mockVersion(record);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setCurrent(true);
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);
		versionRepository.saveAndFlush(version);

		// when process
		TransformJSONLDTask task = new TransformJSONLDTask(record, versionService, schemaService);
		task.run();

		Record actual = recordService.findById(record.getId().toString());

		// a new version is created and it's json-ld with content
		assertThat(actual).isNotNull();
		assertThat(actual.getCurrentVersions()).hasSize(2);

		Version actualVersion = versionService.findVersionForRecord(actual, SchemaService.JSONLD);
		assertThat(actualVersion.isCurrent()).isTrue();
		assertThat(actualVersion).isNotNull();
		assertThat(actualVersion.getContent()).isNotEmpty();

		// if process again, there shouldn't be any change
		(new TransformJSONLDTask(record, versionService, schemaService)).run();
		Record processedAgain = recordService.findById(record.getId().toString());
		assertThat(processedAgain).isNotNull();
		assertThat(processedAgain.getCurrentVersions()).hasSize(2);
	}

}