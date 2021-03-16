package ardc.cerium.oai.service;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.oai.exception.*;
import ardc.cerium.oai.response.OAIIdentifyResponse;
import ardc.cerium.oai.response.OAIResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OAIPMHService.class, ApplicationProperties.class, SchemaService.class })
@TestPropertySource(properties="app.oai.enabled=true")
class OAIPMHServiceTest {

	@Autowired
	OAIPMHService service;

	@MockBean
	VersionService versionService;

	@MockBean
	RecordService recordService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	SchemaService schemaService;

	@Test
	void isOAIProvider() {
		Schema schema1 = schemaService.getSchemaByID(SchemaService.ARDCv1);
		if (service.isOAIProvider(schema1)) {
			String thenamespace = schema1.getNamespace();
			assertThat(thenamespace).isEqualTo("https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc");
		}
	}

	@Test
	void isOAIProvider_false() {
		Schema schema1 = schemaService.getSchemaByID(SchemaService.CSIROv3);
		if (!service.isOAIProvider(schema1)) {
			String theSchemaId = schema1.getId();
			assertThat(theSchemaId).isEqualTo(SchemaService.CSIROv3);
		}
	}

	@Test
	void getOAIProviders() {
		List<Schema> oaiSchemas = service.getOAIProviders();
		for (Schema schema : oaiSchemas) {
			assertThat(service.isOAIProvider(schema));
		}
	}

	@Test
	void isValidVerb() {
		String verb = "ListRecords";
		assertThat(service.isValidVerb(verb)).isTrue();
	}

	@Test
	void isValidVerb_false() {
		assertThat(service.isValidVerb("nonsenseVerb")).isFalse();
	}

	@Test
	void identify() {
		OAIIdentifyResponse response = (OAIIdentifyResponse) service.identify();
		assertThat(response).isInstanceOf(OAIResponse.class);
		assertThat(response.getIdentify().getRepositoryName()).isEqualTo(applicationProperties.getName());
	}

	@Test
	void getRecordException() {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(null, UUID.randomUUID().toString());
		});

		Assert.assertThrows(CannotDisseminateFormatException.class, () -> {
			service.getRecord("nonsense", UUID.randomUUID().toString());
		});

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(SchemaService.ARDCv1, null);
		});

		Assert.assertThrows(IdDoesNotExistException.class, () -> {
			service.getRecord(SchemaService.ARDCv1, "nonexistentIdentifier");
		});
	}

	@Test
	void getRecord() {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(null, UUID.randomUUID().toString());
		});

		Assert.assertThrows(CannotDisseminateFormatException.class, () -> {
			service.getRecord("nonsense", UUID.randomUUID().toString());
		});

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.getRecord(SchemaService.ARDCv1, null);
		});

		Assert.assertThrows(IdDoesNotExistException.class, () -> {
			service.getRecord(SchemaService.ARDCv1, "nonexistentIdentifier");
		});
	}

	@Test
	void listRecords() throws JsonProcessingException {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listRecords(null, null, null, null);
		});

		try {
			OAIResponse response = (OAIResponse) service.listRecords("nonsense", null, null, null);
		}
		catch (CannotDisseminateFormatException e) {
			assertThat(e.getCode()).isEqualTo("cannotDisseminateFormat");
			assertThat(e.getMessageID()).isEqualTo("oai.error.cannot-disseminate-format");
		}

	}

	@Test
	void listIdentifiers() {
		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listIdentifiers(null, null, null, null);
		});

		Assert.assertThrows(CannotDisseminateFormatException.class, () -> {
			service.listIdentifiers("nonsense", null, null, null);
		});

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listIdentifiers(null, null, null, null);
		});

		Assert.assertThrows(BadResumptionTokenException.class, () -> {
			service.listIdentifiers("oai_dc", "garbage", null, null);
		});
	}

	@Test
	void listSets() {
		Assert.assertThrows(NoSetHierarchyException.class, () -> {
			service.listSets();
		});

	}

	@Test
	void listRecords_invalidfrom_throwsBadArgumentException() throws JsonProcessingException {

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listRecords("oai_dc", null, "2020/05/04", null);
		});

	}

	@Test
	void listRecords_invaliduntil_throwsBadArgumentException() throws JsonProcessingException {

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listRecords("oai_dc", null, null, "2020/05/04");
		});

	}

	@Test
	void listIdentifiers_invalidfrom_throwsBadArgumentException() throws JsonProcessingException {

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listIdentifiers("oai_dc", null, "2020/05/04", null);
		});

	}

	@Test
	void listIdentifiers_invaliduntil_throwsBadArgumentException() throws JsonProcessingException {

		Assert.assertThrows(BadArgumentException.class, () -> {
			service.listIdentifiers("oai_dc", null, null, "2020/05/04");
		});

	}

	@Test
	void listIdentifiers_invalidresumptionToken_throwsBadResumptionTokenException() throws JsonProcessingException {

		Assert.assertThrows(BadResumptionTokenException.class, () -> {
			service.listIdentifiers("oai_dc", "garbage", null, null);
		});

	}

	@Test
	void NoMetadataFormatsExistException() throws JsonProcessingException {
		NoMetadataFormatsException noMetadataFormatsException = new NoMetadataFormatsException();
		assertThat(noMetadataFormatsException.getCode()).isEqualTo("noMetadataFormats");
		assertThat(noMetadataFormatsException.getMessageID()).isEqualTo("oai.error.no-metadata-formats");
	}

}