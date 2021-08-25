package ardc.cerium.mycelium.scenario;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import ardc.cerium.mycelium.service.RelationLookupService;
import ardc.cerium.mycelium.task.ImportTask;
import org.junit.jupiter.api.*;
import org.modelmapper.ModelMapper;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import({ MyceliumService.class, GraphService.class, VertexMapper.class, ModelMapper.class, EdgeDTOMapper.class, RelationLookupService.class})
@DataNeo4jTest
public class Scenario_1_IT{

	@Autowired
	MyceliumService myceliumService;

	@Autowired
	RelationLookupService relationLookupService;

	@MockBean
	RequestService requestService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	private static Neo4j embeddedDatabaseServer;

	@BeforeAll
	static void initializeNeo4j() {
		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
				.withDisabledServer()
				.build();
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
	@Disabled("Disabled until JSON payload import is completed")
	@Test
	@DisplayName("Test Graph Scenario 1")
	void scenario1_graphGeneration() {

		// import
		Request request = new Request();
		request.setType(MyceliumService.IMPORT_REQUEST_TYPE);
		request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/scenarios/1_RelationshipScenario/party_1.json");
		ImportTask importTask = new ImportTask(request, myceliumService, myceliumSideEffectService);
		importTask.run();

		request = new Request();
		request.setType(MyceliumService.IMPORT_REQUEST_TYPE);
		request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/scenarios/1_RelationshipScenario/collection_1.json");
		importTask = new ImportTask(request, myceliumService, myceliumSideEffectService);
		importTask.run();

		// AUTCollection1R hasCollector http://nla.gov.au/nla.party-AUTR1
		List<SearchCriteria> criteriaList = new ArrayList<>();
		criteriaList.add(new SearchCriteria("fromIdentifierValue", "AUTCollection1R", SearchOperation.EQUAL));
		Page<Relationship> result1 = myceliumService.search(criteriaList,  PageRequest.of(0, 10));

		assertThat(result1.getNumberOfElements()).isEqualTo(1);
		assertThat(result1.getContent().get(0).getFrom().getIdentifier()).isEqualTo("AUTCollection1R");
		assertThat(result1.getContent().get(0).getTo().getIdentifier()).isEqualTo("http://nla.gov.au/nla.party-AUTR1");
		assertThat(result1.getContent().get(0).getRelations().size()).isEqualTo(1);
		assertThat(result1.getContent().get(0).getRelations().get(0).getType()).isEqualTo("hasCollector");

		// (reverse) http://nla.gov.au/nla.party-AUTR1 isCollectorOf AUTCollection1R
		List<SearchCriteria> criteriaList2 = new ArrayList<>();
		criteriaList2.add(new SearchCriteria("fromIdentifierValue", "http://nla.gov.au/nla.party-AUTR1", SearchOperation.EQUAL));
		Page<Relationship> result2 = myceliumService.search(criteriaList2,  PageRequest.of(0, 10));

		assertThat(result2.getNumberOfElements()).isEqualTo(1);
		assertThat(result2.getContent().get(0).getFrom().getIdentifier()).isEqualTo("http://nla.gov.au/nla.party-AUTR1");
		assertThat(result2.getContent().get(0).getTo().getIdentifier()).isEqualTo("AUTCollection1R");
		assertThat(result2.getContent().get(0).getRelations().size()).isEqualTo(1);
		assertThat(result2.getContent().get(0).getRelations().get(0).getType()).isEqualTo("isCollectorOf");
	}
}
