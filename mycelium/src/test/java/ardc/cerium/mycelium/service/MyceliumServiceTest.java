package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DataNeo4jTest
@Import({ MyceliumService.class, GraphService.class, VertexMapper.class, ModelMapper.class, EdgeDTOMapper.class })
class MyceliumServiceTest {

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
	void getDuplicateRegistryObjectTest() {
		// given A, B, C isSameAs identifier I1
		Graph graph = new Graph();
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		Vertex i1 = new Vertex("I1", "local");
		i1.addLabel(Vertex.Label.Identifier);
		graph.addVertex(a, b, c, i1);

		graph.addEdge(new Edge(a, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(c, i1, RIFCSGraphProvider.RELATION_SAME_AS));

		graphService.ingestGraph(graph);

		// when getDuplicate of A
		Collection<Vertex> duplicates = myceliumService.getDuplicateRegistryObject(a);

		// there are 3 duplicates, A, B and C
		assertThat(duplicates.size()).isEqualTo(3);

		// all of them should be RegistryObject, no identifier allowed
		assertThat(duplicates.stream().allMatch(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))).isTrue();

		// B and C turns up
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("B"))).isTrue();
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("C"))).isTrue();
	}

	@Test
	void getDuplicateMultipleStep() {
		// given A isSameAs I1
		// B isSameAs I1 and isSameAs I2
		// C isSameAs I2
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		Vertex i1 = new Vertex("I1", "local");
		i1.addLabel(Vertex.Label.Identifier);
		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		Vertex i2 = new Vertex("I2", "local");
		i2.addLabel(Vertex.Label.Identifier);
		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);

		Graph graph = new Graph();

		graph.addVertex(a, b, c, i1, i2);

		// (a)-[:isSameAs]->(i1)<-[:isSameAs]-(b)-[:isSameAs]->(i2)<-[:isSameAs]-(c)
		graph.addEdge(new Edge(a, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i2, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(c, i2, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// A, B and C are duplicates

		// when getDuplicate of A
		Collection<Vertex> duplicates = myceliumService.getDuplicateRegistryObject(a);

		// there are 3 duplicates, A, B and C
		assertThat(duplicates.size()).isEqualTo(3);

		// B and C turns up
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("B"))).isTrue();
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("C"))).isTrue();

		// B and C should also have the same amount of duplicates
		assertThat(myceliumService.getDuplicateRegistryObject(b).size()).isEqualTo(3);
		assertThat(myceliumService.getDuplicateRegistryObject(c).size()).isEqualTo(3);
	}

	@Test
	void getRecordState_noRegistryObjectInGraph_null() {
		assertThat(myceliumService.getRecordState("123")).isNull();
	}

	@Test
	void getRecordState_registryObjectExists_notNull() {

		// given a vertex
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setTitle("Test Object");
		Graph graph = new Graph();
		graph.addVertex(a);

		graphService.ingestGraph(graph);

		// when obtain state
		RecordState state = myceliumService.getRecordState("A");

		// not null & has the same title
		assertThat(state).isNotNull();
		assertThat(state.getTitle()).isEqualTo("Test Object");
	}

	@Test
	void getRecordState() {
		// given (c)-[isSameAs]->(i1)<-[isSameAs]-(a)-[isPartOf]->(b) and
		// (b)-[hasPart]->(a)
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setTitle("A");

		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		b.setTitle("B");

		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setTitle("C");

		Vertex i1 = new Vertex("I1", "local");
		i1.addLabel(Vertex.Label.RegistryObject);
		i1.setTitle("I1");

		Graph graph = new Graph();
		graph.addVertex(a, b, c, i1);
		graph.addEdge(new Edge(c, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(a, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(a, b, "isPartOf"));
		graph.addEdge(new Edge(b, a, "hasPart"));

		graphService.ingestGraph(graph);

		// when obtain state
		RecordState state = myceliumService.getRecordState("A");

		assertThat(state.getTitle()).isEqualTo("A");

		// there should be 3 identical, A, C and I1
		assertThat(state.getIdentical().size()).isEqualTo(3);
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("A"))).isTrue();
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("C"))).isTrue();
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("I1"))).isTrue();

		// there should be 1 outbounds, (a)-[isPartOf]->(b)
		assertThat(state.getOutbounds().size()).isEqualTo(1);
		Relationship aToB = state.getOutbounds().get(0);
		assertThat(aToB.getFrom().getIdentifier()).isEqualTo("A");
		assertThat(aToB.getTo().getIdentifier()).isEqualTo("B");
		assertThat(aToB.getRelations().get(0).getType()).isEqualTo("isPartOf");

		assertThat(state).isNotNull();
	}
}