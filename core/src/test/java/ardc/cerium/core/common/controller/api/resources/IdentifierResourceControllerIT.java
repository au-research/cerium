package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.KeycloakIntegrationTest;
import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.exparity.hamcrest.date.DateMatchers.sameDay;

class IdentifierResourceControllerIT extends KeycloakIntegrationTest {

	@Autowired
	RecordRepository recordRepository;

	private final String resourceBaseUrl = "/api/resources/identifiers/";

	@Test
	void store_NotLoggedIn_401() {
		// when POST without logging in, 401
		this.webTestClient.post().uri(resourceBaseUrl).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void store_RecordNotFound_404() {
		// given a request dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(UUID.randomUUID());

		// when POST, record is not found
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), IdentifierDTO.class).exchange().expectStatus().isNotFound();
	}

	@Test
	void store_Forbidden_403() {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		// given a request dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());

		// when POST, oepration is not forbidden
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), IdentifierDTO.class).exchange().expectStatus().isForbidden();
	}

	@Test
	void store_Valid_201() {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// given a request dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());
		dto.setType(Identifier.Type.IGSN);
		dto.setValue("Some value");
		dto.setStatus(Identifier.Status.PENDING);

		// when POST, oepration is not forbidden
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), IdentifierDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody().jsonPath("$.id").exists().jsonPath("$.createdAt").exists();
	}

	@Test
	void store_ImportScope_201Overwrite() throws ParseException {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// given a request dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());
		Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
		dto.setCreatedAt(expectedDate);

		// when POST, expects 201, Location Header, and the ID in the body
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), IdentifierDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody().jsonPath("$.id").exists().jsonPath("$.createdAt").exists()
				.jsonPath("$.createdAt", sameDay(expectedDate)).hasJsonPath();
	}

}