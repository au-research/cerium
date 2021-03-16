package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.KeycloakIntegrationTest;
import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.service.SchemaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.exparity.hamcrest.date.DateMatchers.sameDay;

class RecordResourceControllerIT extends KeycloakIntegrationTest {

	String baseUrl = "/api/resources/records";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	VersionRepository versionRepository;

	@Test
	void index_NotLoggedIn_401() {
		this.webTestClient.get().uri("/api/resources/records/").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void index_hasRecords_showRecords() {
		// given an owned record
		Record record = TestHelper.mockRecord();
		record.setOwnerID(UUID.fromString(userID));
		record.setOwnerType(Record.OwnerType.User);
		recordRepository.saveAndFlush(record);

		// and a not owned record
		Record notOwned = TestHelper.mockRecord();
		recordRepository.saveAndFlush(notOwned);

		// when get, there should be 1 records shown
		this.webTestClient.get().uri(baseUrl).header("Authorization", getBasicAuthenticationHeader(username, password))
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1);
	}

	@Test
	void index_hasTitle_filterByTitle() {
		// given a target record
		Record record = TestHelper.mockRecord();
		record.setOwnerID(UUID.fromString(userID));
		record.setOwnerType(Record.OwnerType.User);
		record.setTitle("Search me");
		recordRepository.saveAndFlush(record);

		// and a falsy record
		Record other = TestHelper.mockRecord();
		record.setOwnerID(UUID.fromString(userID));
		record.setOwnerType(Record.OwnerType.User);
		record.setTitle("not me");
		recordRepository.saveAndFlush(other);

		// when get, there should be 1 records shown
		this.webTestClient.get().uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("title", "Search me").build())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1);
	}

	// todo index_hasRecords_showRecords()
	// todo index_hasManyRecords_showPagination()

	@Test
	void show_NotLoggedIn_401() {
		// given a record
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		// when show without permission, get 401
		this.webTestClient.get().uri(String.format("/api/resources/records/%s", record.getId())).exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void show_recordDoesNotExist_404() {
		this.webTestClient.get().uri(String.format("/api/resources/records/%s", UUID.randomUUID()))
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isNotFound();
	}

	@Test
	void show_recordExist_200() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setOwnerID(UUID.fromString(userID));
		record.setOwnerType(Record.OwnerType.User);
		recordRepository.saveAndFlush(record);

		// when show with authentication, gets 200 and the record
		this.webTestClient.get().uri(String.format("/api/resources/records/%s", record.getId()))
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isOk().expectBody().jsonPath("$.id").isEqualTo(record.getId().toString()).jsonPath("$.visible")
				.isEqualTo(record.isVisible()).jsonPath("$.createdAt").exists();
	}

	@Test
	void create_NotLoggedIn_401() {
		// when POST without logging in, 401
		this.webTestClient.post().uri("/api/resources/records/").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void create_InsufficientPermission_403() {
		// the request is for a different allocation that the user did not have access to
		RecordDTO requestDTO = new RecordDTO();
		requestDTO.setAllocationID(UUID.randomUUID());

		// when POST, expects 403
		this.webTestClient.post().uri("/api/resources/records/")
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isForbidden();
	}

	@Test
	void create_SufficientPermission_201WithLocation() {
		// request a record with the associated allocation
		RecordDTO requestDTO = new RecordDTO();
		requestDTO.setAllocationID(UUID.fromString(resourceID));

		// when POST, expects 201, Location Header, and the record ID in the body
		this.webTestClient.post().uri("/api/resources/records/")
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody().jsonPath("$.id").exists().jsonPath("$.createdAt").exists();
	}

	@Test
	void create_ImportPermission_201OverwriteData() throws ParseException {
		// this test requires the provided credentials have ardc.cerium.core.igsn:import scope against the
		// allocation

		// request a record with the associated allocation, with the overwritten createdAt
		// date
		RecordDTO requestDTO = new RecordDTO();
		requestDTO.setAllocationID(UUID.fromString(resourceID));
		Date expectedDate = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
		requestDTO.setCreatedAt(expectedDate);

		// when POST, expects 201, Location Header, and the record ID in the body
		this.webTestClient.post().uri("/api/resources/records/")
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isCreated().expectHeader()
				.exists("Location").expectBody().jsonPath("$.id").exists().jsonPath("$.createdAt").exists()
				.jsonPath("$.createdAt", sameDay(expectedDate)).hasJsonPath();
	}

	@Test
	void update_NotLoggedIn_401() {
		// given a record
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		// when PUT without logged in, 401
		this.webTestClient.put().uri("/api/resources/records/" + record.getId().toString()).exchange().expectStatus()
				.isUnauthorized();
	}

	@Test
	void update_RecordNotFound_404() {
		// the request is to update the creatorID
		RecordDTO requestDTO = new RecordDTO();

		// when PUT without logged in, 401
		this.webTestClient.put().uri("/api/resources/records/" + UUID.randomUUID().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isNotFound();
	}

	@Test
	void update_InsufficientPermission_403() {
		// given a record with a random Allocation
		Record record = TestHelper.mockRecord();
		record.setAllocationID(UUID.randomUUID());
		recordRepository.saveAndFlush(record);

		// the request is to update the creatorID
		RecordDTO requestDTO = new RecordDTO();
		requestDTO.setCreatorID(UUID.randomUUID());

		// when PUT with default credentials (does not have access to that allocation),
		// 403
		// todo refactor update will affect this
		this.webTestClient.put().uri("/api/resources/records/" + record.getId().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isForbidden();
	}

	@Test
	void update_SufficientPermission_202() {
		// given a record with a the same allocation as the credentials
		Record record = TestHelper.mockRecord();
		record.setAllocationID(UUID.fromString(resourceID));
		record.setVisible(true);
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// the request is to update the status
		RecordDTO requestDTO = new RecordDTO();
		requestDTO.setVisible(false);

		// when PUT with default credentials, 202, status is updated
		this.webTestClient.put().uri("/api/resources/records/" + record.getId().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password))
				.body(Mono.just(requestDTO), RecordDTO.class).exchange().expectStatus().isAccepted().expectBody()
				.jsonPath("$.id").isEqualTo(record.getId().toString()).jsonPath("$.visible").isEqualTo(false);
	}

	@Test
	void delete_NotLoggedIn_401() {
		// given a record
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);

		// when DELETE without logging in, 401
		this.webTestClient.post().uri("/api/resources/records/" + record.getId().toString()).exchange().expectStatus()
				.isUnauthorized();
	}

	@Test
	void delete_RecordNotFound_404() {
		// when DELETE with credentials on a non existence record, expects 404
		this.webTestClient.delete().uri("/api/resources/records/" + UUID.randomUUID().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isNotFound();
	}

	@Test
	void delete_InsufficientPermission_403() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setOwnerID(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		recordRepository.saveAndFlush(record);

		// when DELETE with credentials, expects 403
		this.webTestClient.delete().uri("/api/resources/records/" + record.getId().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isForbidden();
	}

	@Test
	void delete_SufficientPermission_202() {
		// given a record owned by the user
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// when DELETE with credentials, expects 202
		this.webTestClient.delete().uri("/api/resources/records/" + record.getId().toString())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isAccepted();
	}

	@Test
	void showVersions() {
		// given a record owned by the user
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		// and 2 version
		Version version1 = TestHelper.mockVersion();
		version1.setRecord(record);
		version1.setCurrent(true);
		version1.setSchema(SchemaService.ARDCv1);
		versionRepository.saveAndFlush(version1);

		Version version2 = TestHelper.mockVersion();
		version2.setRecord(record);
		version2.setCurrent(false);
		version2.setSchema(SchemaService.CSIROv3);
		versionRepository.saveAndFlush(version2);

		// 2 versions when shown
		this.webTestClient.get().uri(baseUrl + "/" + record.getId().toString() + "/versions")
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(2);

		// 1 version when filter by schema
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + "/" + record.getId().toString() + "/versions")
						.queryParam("schema", SchemaService.ARDCv1).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1);
	}

	@AfterEach
	void afterEach() {
		versionRepository.deleteAll();
		recordRepository.deleteAll();
	}

}