package ardc.cerium.core.common.controller.api.services;

import ardc.cerium.core.KeycloakIntegrationTest;
import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.EmbargoRepository;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

class AuthenticationCheckControllerIT extends KeycloakIntegrationTest {

	final String baseUrl = "/api/services/";

	@Autowired
	RecordRepository recordRepository;

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	EmbargoRepository embargoRepository;

	@Test
	void validateOwnership_notLoggedIn_401() {
		this.webTestClient.get().uri(baseUrl + "auth-check/").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void validateOwnership_loggedInNoIdentifier_404() {
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + "auth-check/")
						.queryParam("identifier", "10273/XXAA" + UUID.randomUUID().toString()).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isNotFound();
	}

	@Test
	void validateOwnership_loggedInNotOwned_403() {
		// given a record, and an identifier
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + "auth-check/").queryParam("identifier", identifier.getValue()).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isForbidden();
	}

	@Test
	void validateOwnership_loggedInOwned_200() {
		// given an owned record, and an identifier
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		recordRepository.saveAndFlush(record);

		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl + "auth-check/").queryParam("identifier", identifier.getValue()).build())
				.header("Authorization", getBasicAuthenticationHeader(username, password)).exchange().expectStatus()
				.isOk();
	}

	@Test
	void isPrivate_403() {
		// given an owned record, and an identifier
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		record.setVisible(false);
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl+ "isPublic/").queryParam("identifier", identifier.getValue()).build())
				.exchange().expectStatus().isForbidden();
	}

	@Test
	void isPublic_200() {
		// given an owned record, and an identifier
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		record.setVisible(true);
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl+ "isPublic/").queryParam("identifier", identifier.getValue()).build())
				.exchange().expectStatus().isOk();
	}

	@Test
	void hasEmbargo_200() {
		// given an owned record, and an identifier and an embargo date
		Date embargoDate = Helpers.convertDate("2020-10-30");
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		record.setVisible(false);
		recordRepository.saveAndFlush(record);

		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		Embargo embargo = TestHelper.mockEmbargo(record);
		embargo.setEmbargoEnd(embargoDate);
		embargoRepository.saveAndFlush(embargo);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl+ "hasEmbargo/").queryParam("identifier", identifier.getValue()).build())
				.exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("2020-10-30 00:00:00.0");
	}

	@Test
	void hasNoEmbargo_200() {
		// given an owned record, and an identifier with no embargo
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.fromString(userID));
		record.setVisible(false);
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue("10273/XXAA" + UUID.randomUUID().toString());
		identifierRepository.saveAndFlush(identifier);

		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl+ "hasEmbargo/").queryParam("identifier", identifier.getValue()).build())
				.exchange().expectStatus().isOk().expectBody().equals(null);
	}

}