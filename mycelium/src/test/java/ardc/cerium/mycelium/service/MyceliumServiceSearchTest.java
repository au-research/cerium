package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataNeo4jTest
@Import({ MyceliumService.class, GraphService.class, VertexMapper.class, ModelMapper.class, EdgeDTOMapper.class })
public class MyceliumServiceSearchTest {

	private static Neo4j embeddedDatabaseServer;

	@Autowired
	MyceliumService myceliumService;

	@Autowired
	GraphService graphService;

	@MockBean
	RequestService requestService;

	@BeforeAll
	static void initializeNeo4j() {

		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder().withDisabledServer().build();
	}

	@AfterAll
	static void stopNeo4j() {
		embeddedDatabaseServer.close();
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", () -> null);
	}

	@Test
	@DisplayName("isSameAs to identifier relation is excluded from searching by default")
	void isSameAsMustNotReturn() throws IOException {
		// given an ingest
		String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
		myceliumService.ingest(rifcs);

		// when search
		List<SearchCriteria> criteriaList = new ArrayList<>();
		criteriaList
				.add(new SearchCriteria("fromIdentifierValue", "AUTestingRecords2DCIRecords6", SearchOperation.EQUAL));
		criteriaList.add(new SearchCriteria("fromIdentifierType", "ro:key", SearchOperation.EQUAL));
		Page<Relationship> relationships = myceliumService.search(criteriaList, PageRequest.of(0, 5));
		assertThat(relationships).isNotNull();

		// relation to local identifier AODN must not return
		assertThat(relationships.getContent().stream()
				.filter(relationship -> relationship.getTo().getIdentifier().contains("AODN")).findAny().orElse(null))
						.isNull();
		assertThat(relationships).isNotNull();
	}

	@Test
	@DisplayName("A search provides a good set of Pagination data")
	void genericSearch() throws IOException {
		// given an ingest
		String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
		myceliumService.ingest(rifcs);

		// when search
		List<SearchCriteria> criteriaList = new ArrayList<>();
		criteriaList
				.add(new SearchCriteria("fromIdentifierValue", "AUTestingRecords2DCIRecords6", SearchOperation.EQUAL));
		criteriaList.add(new SearchCriteria("fromIdentifierType", "ro:key", SearchOperation.EQUAL));
		Page<Relationship> relationships = myceliumService.search(criteriaList, PageRequest.of(0, 5));
		assertThat(relationships).isNotNull();

		// expects relationships to be a page, the from & to are Vertices
		assertThat(relationships).isInstanceOf(PageImpl.class);
		assertThat(relationships.getContent().size()).isGreaterThan(0);
		assertThat(relationships.getContent().get(0).getFrom()).isInstanceOf(Vertex.class);
		assertThat(relationships.getContent().get(0).getFrom().getIdentifier())
				.isEqualTo("AUTestingRecords2DCIRecords6");
		assertThat(relationships.getContent().get(0).getFrom().getIdentifierType())
				.isEqualTo(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		assertThat(relationships.getContent().get(0).getTo()).isInstanceOf(Vertex.class);
		assertThat(relationships.getContent().get(0).getRelations().size()).isGreaterThan(0);
		assertThat(relationships.getContent().get(0).getRelations().get(0)).isInstanceOf(EdgeDTO.class);
	}

	@Test
	void duplicateRelationshipSearchTest() throws IOException {

		// given an ingest of duplicate records
		String rifcs = Helpers.readFile("src/test/resources/rifcs/duplicate_records.xml");
		myceliumService.ingest(rifcs);

		List<SearchCriteria> criteriaList = new ArrayList<>();
		criteriaList.add(new SearchCriteria("fromIdentifierValue", "C1", SearchOperation.EQUAL));
		Page<Relationship> relationships = myceliumService.search(criteriaList, PageRequest.of(0, 5));

		// C1 isPartOf C3
		Relationship C1C3 = relationships.stream()
				.filter(relationship -> relationship.getTo().getIdentifier().equals("C3")).findFirst().orElse(null);
		assertThat(C1C3).isNotNull();
		assertThat(C1C3.getRelations().stream().allMatch(edgeDTO -> edgeDTO.getType().equals("isPartOf"))).isTrue();

		// C1 isProducedBy A1 (via duplicate)
		Relationship C1A1 = relationships.stream()
				.filter(relationship -> relationship.getTo().getIdentifier().equals("A1")).findFirst().orElse(null);
		assertThat(C1A1).isNotNull();
		assertThat(C1A1.getRelations().stream().allMatch(edgeDTO -> edgeDTO.getType().equals("isProducedBy"))).isTrue();

		// C1 isProducedBy A2 (via duplicate)
		Relationship C1A2 = relationships.stream()
				.filter(relationship -> relationship.getTo().getIdentifier().equals("A2")).findFirst().orElse(null);
		assertThat(C1A2).isNotNull();
		assertThat(C1A2.getRelations().stream().allMatch(edgeDTO -> edgeDTO.getType().equals("isProducedBy"))).isTrue();

		// C1 hasAssociationWith A3 (via 2 step duplicate)
		Relationship C1A3 = relationships.stream()
				.filter(relationship -> relationship.getTo().getIdentifier().equals("A3")).findFirst().orElse(null);
		assertThat(C1A3).isNotNull();
		assertThat(C1A3.getRelations().stream().allMatch(edgeDTO -> edgeDTO.getType().equals("hasAssociationWith")))
				.isTrue();
	}

}
