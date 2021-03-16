package ardc.cerium.core.common.controller.api.pub;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.WebIntegrationTest;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.repository.RecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class IdentifiersPublicControllerIT extends WebIntegrationTest {

	private final String baseUrl = "/api/public/identifiers/";

	@Autowired
	IdentifierRepository identifierRepository;

	@Autowired
	RecordRepository recordRepository;

	@Test
	void index_identifiers_showAllWithPagination() {
		// given 5 identifiers
		for (int i = 0; i < 5; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.saveAndFlush(record);
			Identifier identifier = TestHelper.mockIdentifier(record);
			identifierRepository.saveAndFlush(identifier);
		}
		identifierRepository.flush();

		// when get all
		this.webTestClient.get().uri(baseUrl).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.numberOfElements").isEqualTo(5);

		// when get paginated (total of 3 pages when page size is 2, there are 5 total
		// identifiers)
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("page", 0).queryParam("size", 2).build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(2)
				.jsonPath("$.totalPages").isEqualTo(3);
	}

	@Test
	void index_filterByTypeAndValue_returnCorrectResult() {
		// given 3 random identifiers of type IGSN
		for (int i = 0; i < 5; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.saveAndFlush(record);
			Identifier identifier = TestHelper.mockIdentifier(record);
			identifier.setValue(UUID.randomUUID().toString());
			identifier.setType(Identifier.Type.IGSN);
			identifierRepository.saveAndFlush(identifier);
		}

		// and 1 identifier used for searching
		Record record = TestHelper.mockRecord();
		recordRepository.saveAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue(UUID.randomUUID().toString());
		identifier.setType(Identifier.Type.IGSN);
		identifierRepository.saveAndFlush(identifier);

		// expects 6 identifiers of type IGSN
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("type", Identifier.Type.IGSN).build()).exchange()
				.expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(6);

		// expects 1 identifier has the right (random) value
		this.webTestClient.get()
				.uri(uriBuilder -> uriBuilder.path(baseUrl).queryParam("value", identifier.getValue()).build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("$.numberOfElements").isEqualTo(1);
	}

	@BeforeEach
	@AfterEach
	void clear() {
		identifierRepository.deleteAll();
		identifierRepository.flush();
	}

}