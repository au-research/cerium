//package ardc.cerium.core.ardc.cerium.core.processor;
//
//import ardc.cerium.core.TestHelper;
//import ardc.cerium.core.WebIntegrationTest;
//import ardc.cerium.core.ardc.cerium.core.common.entity.Record;
//import ardc.cerium.core.ardc.cerium.core.common.entity.Version;
//import ardc.cerium.core.ardc.cerium.core.common.repository.RecordRepository;
//import ardc.cerium.core.ardc.cerium.core.common.repository.VersionRepository;
//import ardc.cerium.core.ardc.cerium.core.common.service.RecordService;
//import ardc.cerium.core.ardc.cerium.core.common.service.SchemaService;
//import ardc.cerium.core.ardc.cerium.core.common.service.VersionService;
//import ardc.cerium.core.ardc.cerium.core.common.util.Helpers;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class RecordTransformLDProcessorIT extends WebIntegrationTest {
//
//	@Autowired
//	VersionService versionService;
//
//	@Autowired
//	RecordService recordService;
//
//	@Autowired
//	SchemaService schemaService;
//
//	@Autowired
//	private RecordRepository recordRepository;
//
//	@Autowired
//	private VersionRepository versionRepository;
//
//	@Test
//	void process_createsNewVersion() throws Exception {
//
//		// given a record with a current version with schema ardcv1
//		Record record = TestHelper.mockRecord();
//		recordRepository.saveAndFlush(record);
//
//		Version version = TestHelper.mockVersion(record);
//		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
//		version.setCurrent(true);
//		version.setContent(validXML.getBytes());
//		version.setSchema(SchemaService.ARDCv1);
//		versionRepository.saveAndFlush(version);
//
//		// when process
//		RecordTransformLDProcessor processor = new RecordTransformLDProcessor(versionService, recordService,
//				schemaService);
//		Record actual = processor.process(record);
//
//		// a new version is created and it's json-ld with content
//		assertThat(actual).isNotNull();
//		assertThat(actual.getCurrentVersions()).hasSize(2);
//
//		Version actualVersion = versionService.findVersionForRecord(actual, SchemaService.JSONLD);
//		assertThat(actualVersion.isCurrent()).isTrue();
//		assertThat(actualVersion).isNotNull();
//		assertThat(actualVersion.getContent()).isNotEmpty();
//
//		// if process again, there shouldn't be any change
//		Record processedAgain = processor.process(record);
//		assertThat(processedAgain).isNotNull();
//		assertThat(processedAgain.getCurrentVersions()).hasSize(2);
//	}
//
//}