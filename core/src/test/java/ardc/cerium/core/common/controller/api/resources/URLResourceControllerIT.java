package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.KeycloakIntegrationTest;
import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.URLDTO;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.exparity.hamcrest.date.DateMatchers.sameDay;

class URLResourceControllerIT extends KeycloakIntegrationTest {

	@Autowired
	RecordRepository recordRepository;

	private final String resourceBaseUrl = "/api/resources/urls/";

	@Test
	void store_NotLoggedIn_401() {
		this.webTestClient.post().uri(resourceBaseUrl).exchange().expectStatus().isUnauthorized();
	}

	// todo store_RecordNotOwned_403
	// todo store_ValidRecord_202
	// todo store_ImportScope_202Overwrite

	// todo store_RecordNotFound_404
	@Test
	void store_RecordNotFound_404() {
		// given a request dto
		URLDTO dto = new URLDTO();
		dto.setRecord(UUID.randomUUID());
		dto.setUrl("https://researchdata.edu.au/");

		// when POST, record is not found
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), URLDTO.class).exchange().expectStatus().isNotFound();
	}

	@Test
	void store_Forbidden_403() {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		// given a request dto
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());
		dto.setUrl("https://researchdata.edu.au/");

		// when POST, oepration is not forbidden
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), URLDTO.class).exchange().expectStatus().isForbidden();
	}

	@Test
	void store_Valid_201() {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// given a request dto
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());
		dto.setUrl("https://researchdata.edu.au/");

		// when POST, oepration is not forbidden
		this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(dto), URLDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody().jsonPath("$.id").exists().jsonPath("$.createdAt").exists();
	}

	@Test
	void store_ImportScope_201Overwrite() throws ParseException {
		// given a record of a different owner
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		record.setAllocationID(UUID.fromString(resourceID));
		recordRepository.saveAndFlush(record);

		// given a request dto
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());
		Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
		dto.setCreatedAt(expectedDate);
		dto.setUrl("https://researchdata.edu.au/");

		// when POST, expects 201, Location Header, and the ID in the body
		URLDTO resultDTO = this.webTestClient.post().uri(resourceBaseUrl)
				.header("Authorization", getBasicAuthenticationHeader(adminUsername, adminPassword))
				.body(Mono.just(dto), URLDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody(URLDTO.class).returnResult().getResponseBody();

		// expects the dates to be overwritten
		assertThat(resultDTO).isNotNull();
		assertThat(resultDTO.getId()).isNotNull();
		assertThat(resultDTO.getCreatedAt()).isNotNull();
		assertThat(resultDTO.getCreatedAt()).isNotNull();
		assertThat(resultDTO.getCreatedAt()).isInSameDayAs(expectedDate);
	}

}