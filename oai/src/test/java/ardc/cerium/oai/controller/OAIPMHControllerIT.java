package ardc.cerium.oai.controller;

import ardc.cerium.oai.TestHelper;
import ardc.cerium.oai.WebIntegrationTest;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.oai.service.OAIPMHService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.io.IOException;
import java.util.Date;

@Import({ IdentifierMapper.class })
@TestPropertySource(properties="app.oai.enabled=true")
public class OAIPMHControllerIT extends WebIntegrationTest {

	final String base_url = "/api/services/oai-pmh";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@Autowired
	OAIPMHService service;

	@BeforeEach
	void setUp() {
		versionRepository.flush();
		versionRepository.deleteAll();
		versionRepository.flush();

		recordRepository.flush();
		recordRepository.deleteAll();
		recordRepository.flush();
	}

	@Test
	@DisplayName("Utilises SchemaServices to obtain a list of approved metadataFormats for OAIPMH")
	void handle_verb_ListMetadataFormats_returns() throws Exception {
		this.webTestClient.get().uri(base_url + "?verb=ListMetadataFormats").exchange().expectStatus().isOk()
				.expectBody().xpath("/OAI-PMH/ListMetadataFormats/metadataFormat/schema")
				.isEqualTo("https://identifiers.ardc.edu.au/igsn-schema/descriptive/1.0/resource.xsd");
	}

	@Test
	@DisplayName("Handles verb GetRecord with a no record identifier")
	void handle_verb_GetRecord_no_identifier_returns() {
		this.webTestClient.get().uri(base_url + "?verb=GetRecord&metadataPrefix=ardc-ardc.cerium.core.igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists();
	}

	@Test
	@DisplayName("Handles verb GetRecord with a non existent record identifier")
	void handle_verb_GetRecord_noid() throws Exception {
		this.webTestClient.get()
				.uri(base_url + "?verb=GetRecord&identifier=gjdhgjdfhgjfd&metadataPrefix=ardc-igsn-desc-1.0").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error")
				.isEqualTo("The value of the identifier argument is unknown or illegal in this repository.");
	}

	@Test
	void handle_verb_GetRecord_returnsRecord() throws IOException {
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		String id = record.getId().toString();

		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(validXML.getBytes());
		version.setSchema(SchemaService.ARDCv1);
		versionRepository.saveAndFlush(version);

		this.webTestClient.get()
				.uri(base_url + "?verb=GetRecord&identifier=" + id + "&metadataPrefix=" + SchemaService.ARDCv1)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/GetRecord/record").exists()
				.xpath("/OAI-PMH/GetRecord/record/header").exists()
				.xpath("/OAI-PMH/GetRecord/record/metadata/resources").exists()
				.xpath("/OAI-PMH/GetRecord/record/header/identifier").isEqualTo(id);
	}

	@Test
	void handle_verb_ListRecords_returnsRecords() throws IOException {
		int i = 0;
		Date versionDate = Helpers.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 120; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			record.setType("IGSN");
			recordRepository.save(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(versionDate);
			versionRepository.save(version);
		}

		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build();
		this.webTestClient = this.webTestClient.mutate().exchangeStrategies(exchangeStrategies).build();

		String from = "2020-09-23T09:30:23Z";
		String until = "2020-09-23T09:30:28Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.ARDCv1 + "&from=" + from + "&until="
						+ until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/ListRecords").exists()
				.xpath("/OAI-PMH/ListRecords/record/header").exists()
				.xpath("/OAI-PMH/ListRecords/record/metadata/resources").exists()
				.xpath("/OAI-PMH/ListRecords/resumptionToken[@completeListSize=120]").exists();
	}

	@Test
	void handle_verb_ListRecords_returnsNoRecordsMatchException() throws IOException {
		int i = 0;
		Date versionDate = Helpers.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 150; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			recordRepository.save(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(versionDate);
			versionRepository.save(version);
		}

		String from = "2020-09-23T09:30:28Z";
		String until = "2020-09-23T09:30:22Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.ARDCv1 + "&from=" + from + "&until="
						+ until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists()
				.xpath("/OAI-PMH/error[@code='noRecordsMatch']").exists();
	}

	@Test
	void handle_verb_ListSets_returnsNoSetHierarchyException() {

		this.webTestClient.get().uri(base_url + "?verb=ListSets").exchange().expectStatus().isOk().expectBody()
				.xpath("/OAI-PMH/error").exists().xpath("/OAI-PMH/error[@code='noSetHierarchy']").exists();
	}

	@Test
	void handle_verb_Identify_returnsAllElements() {

		this.webTestClient.get().uri(base_url + "?verb=Identify").exchange().expectStatus().isOk().expectBody()
				.xpath("/OAI-PMH/Identify").exists().xpath("/OAI-PMH/Identify/repositoryName").exists()
				.xpath("/OAI-PMH/Identify/baseURL").exists().xpath("/OAI-PMH/Identify/protocolVersion").exists()
				.xpath("/OAI-PMH/Identify/adminEmail").exists().xpath("/OAI-PMH/Identify/earliestDatestamp").exists()
				.xpath("/OAI-PMH/Identify/deletedRecord").exists().xpath("/OAI-PMH/Identify/granularity").exists();
	}

	@Test
	void handle_verb_ListIdentifiers_returnsNoRecordsMatchException() throws IOException {

		int i = 0;
		Date versionDate = Helpers.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 110; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			record.setType("IGSN");
			recordRepository.saveAndFlush(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(versionDate);
			versionRepository.saveAndFlush(version);
		}

		String from = "2020-09-23T09:30:23Z";
		String until = "2020-09-23T09:30:28Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListIdentifiers&metadataPrefix=" + SchemaService.OAIDC + "&from=" + from
						+ "&until=" + until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists()
				.xpath("/OAI-PMH/error[@code='noRecordsMatch']").exists();
	}

	@Test
	void handle_verb_ListIdentifiers_returnsIdentifiers() throws IOException {

		int i = 0;
		Date versionDate = Helpers.convertDate("2020-09-23T09:30:25Z");
		for (i = 0; i < 110; i++) {
			Record record = TestHelper.mockRecord();
			record.setModifiedAt(versionDate);
			record.setType("IGSN");
			recordRepository.saveAndFlush(record);

			Version version = TestHelper.mockVersion(record);
			version.setCurrent(true);
			String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
			version.setContent(validXML.getBytes());
			version.setSchema(SchemaService.OAIDC);
			version.setCreatedAt(versionDate);
			versionRepository.saveAndFlush(version);
		}

		String from = "2020-09-23T09:30:23Z";
		String until = "2020-09-23T09:30:28Z";
		this.webTestClient.get()
				.uri(base_url + "?verb=ListIdentifiers&metadataPrefix=" + SchemaService.OAIDC + "&from=" + from
						+ "&until=" + until)
				.exchange().expectStatus().isOk().expectBody().xpath("/OAI-PMH/ListIdentifiers").exists()
				.xpath("/OAI-PMH/ListIdentifiers/header").exists().xpath("/OAI-PMH/ListIdentifiers/header/identifier")
				.exists().xpath("/OAI-PMH/ListIdentifiers/header/datestamp").exists()
				.xpath("/OAI-PMH/ListIdentifiers/resumptionToken").exists();
	}

	@Test
	void handle_set_parameter() {

		this.webTestClient.get()
				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.OAIDC + "&set=thisSet").exchange()
				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists()
				.xpath("/OAI-PMH/error[@code='noSetHierarchy']").exists();

	}

//	@Test
//	void logging_to_apiLog() throws IOException {
//
//		Logger LOG = LogManager.getLogger(APILoggingService.class);
//		org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) LOG;
//		Appender appender = loggerImpl.getAppenders().get("API");
//
//		// if for some reason API Appender in integration test environment is null
//		// (different log4j configuration, different logging setup, logging mocks)
//		// if that's the case this test is not useful
//		if (appender == null) {
//			return;
//		}
//
//		String logPath = ((RollingFileAppender) appender).getFileName();
//
//		String expectedQuery = "\"query\":\"verb=ListRecords&metadataPrefix=oai_dc&set=thisSet\"";
//		String expectedResponse = "\"response\":{\"status_code\":\"200\"}";
//		String expectedEvent = "\"event\":{\"category\":\"web\",\"action\":\"api\",\"outcome\":\"success\"}";
//
//		this.webTestClient.get()
//				.uri(base_url + "?verb=ListRecords&metadataPrefix=" + SchemaService.OAIDC + "&set=thisSet").exchange()
//				.expectStatus().isOk().expectBody().xpath("/OAI-PMH/error").exists()
//				.xpath("/OAI-PMH/error[@code='noSetHierarchy']").exists();
//
//		File file = new File(logPath);
//		String log = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//		Assert.assertTrue(log.contains(expectedQuery));
//		Assert.assertTrue(log.contains(expectedResponse));
//		Assert.assertTrue(log.contains(expectedEvent));
//
//	}

}
