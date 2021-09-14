package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RelationDocument;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.util.Neo4jClientBiFunctionHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.neo4j.driver.types.Relationship;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataNeo4jTest
@Import({GraphService.class, VertexMapper.class, EdgeDTOMapper.class, ModelMapper.class})
class GraphServiceTest {

	@Autowired
	GraphService graphService;

	@Autowired
	Neo4jClient neo4jClient;

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

	@Test
	void ingestVertexTest() {
		// given a Vertex
		Vertex vertex = new Vertex("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);

		// when insert
		graphService.ingestVertex(vertex);

		// it exists in neo4j
		Vertex actual = neo4jClient.query("MATCH (n:Vertex {identifier: $identifier, identifierType: $type}) RETURN n")
				.bind("testIdentifier").to("identifier")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("type")
				.fetchAs(Vertex.class)
				.mappedBy(((typeSystem, record) -> Neo4jClientBiFunctionHelper.toVertex(record, "n"))).one()
				.orElse(null);

		assertThat(actual).isNotNull();
		assertThat(actual.getIdentifier()).isEqualTo("testIdentifier");
		assertThat(actual.getIdentifierType()).isEqualTo(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
	}

	@Test
	void ingestEdgeTest() {

		// given 2 nodes and 1 edge between them
		Vertex fromNode = new Vertex("fromNode", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex endNode = new Vertex("endNode", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graphService.ingestVertex(fromNode);
		graphService.ingestVertex(endNode);

		Edge edge = new Edge(fromNode, endNode, "isRelatedTo");

		// when insert the edge
		graphService.ingestEdge(edge);

		// it exists in neo4j
		Collection<Relationship> result = neo4jClient
				.query("MATCH (n:`Vertex` {identifier: $fromID, identifierType: $fromType})-[r]->() RETURN r")
				.bind("fromNode").to("fromID")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("fromType")
				.fetchAs(Relationship.class)
				.mappedBy((typeSystem, record) -> record.get("r").asRelationship())
				.all();
		assertThat(result).isNotNull();

		// only 1 relation between them
		assertThat(result.size()).isEqualTo(1);
		Relationship theRelationship = new ArrayList<>(result).get(0);
		assertThat(theRelationship.hasType("isRelatedTo")).isTrue();

		// the default properties are set
		assertThat(theRelationship.get("reverse")).isNotNull();
		assertThat(theRelationship.get("duplicate")).isNotNull();
		assertThat(theRelationship.get("internal")).isNotNull();
		assertThat(theRelationship.get("public")).isNotNull();

		// the default properties have the default values
		assertThat(theRelationship.get("reverse").asBoolean()).isFalse();
		assertThat(theRelationship.get("duplicate").asBoolean()).isFalse();
		assertThat(theRelationship.get("internal").asBoolean()).isTrue();
		assertThat(theRelationship.get("public").asBoolean()).isTrue();

		// when ingest again (make sure the operation is idempotent
		graphService.ingestEdge(edge);
		graphService.ingestEdge(edge);

		// there still only 1 edge
		Collection<Relationship> resultAgain = neo4jClient
				.query("MATCH (n:`Vertex` {identifier: $fromID, identifierType: $fromType})-[r]->() RETURN r")
				.bind("fromNode").to("fromID")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("fromType")
				.fetchAs(Relationship.class)
				.mappedBy((typeSystem, record) -> record.get("r").asRelationship())
				.all();
		assertThat(resultAgain).isNotNull();
		assertThat(resultAgain.size()).isEqualTo(1);
	}

	@Test
	void ingestGraphTest() {
		// given 2 nodes and 1 edge between them and forms a graph
		Vertex fromNode = new Vertex("fromNode", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex endNode = new Vertex("endNode", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Edge edge = new Edge(fromNode, endNode, "isRelatedTo");

		Graph graph = new Graph();
		graph.addVertex(fromNode);
		graph.addVertex(endNode);
		graph.addEdge(edge);

		// when insert the graph
		graphService.ingestGraph(graph);

		// the path exists
		Collection<RelationDocument> result = neo4jClient
				.query("MATCH p=(n:Vertex {identifier: $fromID, identifierType: $fromIDType})-[r]->() RETURN p")
				.bind("fromNode").to("fromID")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("fromIDType")
				.fetchAs(RelationDocument.class)
				.mappedBy((typeSystem, record) -> Neo4jClientBiFunctionHelper.pathToRelationDocument(record, "p"))
				.all();
		ArrayList<RelationDocument> actual = new ArrayList<>(result);

		assertThat(actual).isNotNull();
		assertThat(actual.size()).isEqualTo(1);
		RelationDocument actualRelation = actual.get(0);
		assertThat(actualRelation.getFromIdentifier()).isEqualTo("fromNode");
		assertThat(actualRelation.getToIdentifier()).isEqualTo("endNode");
		assertThat(actualRelation.getRelationType()).isEqualTo("isRelatedTo");
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
		Collection<Vertex> duplicates = graphService.getDuplicateRegistryObject(a);

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
		Collection<Vertex> duplicates = graphService.getDuplicateRegistryObject(a);

		// there are 3 duplicates, A, B and C
		assertThat(duplicates.size()).isEqualTo(3);

		// B and C turns up
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("B"))).isTrue();
		assertThat(duplicates.stream().anyMatch(vertex -> vertex.getIdentifier().equals("C"))).isTrue();

		// B and C should also have the same amount of duplicates
		assertThat(graphService.getDuplicateRegistryObject(b).size()).isEqualTo(3);
		assertThat(graphService.getDuplicateRegistryObject(c).size()).isEqualTo(3);
	}

	@Test
	void getRecordState_noRegistryObjectInGraph_null() {
		assertThat(graphService.getRecordState("123")).isNull();
	}

	@Test
	void getRecordState_registryObjectExists_notNull() {

		// given a vertex id & key pair
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setTitle("Test Object");

		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.Identifier);

		Graph graph = new Graph();
		graph.addVertex(a, b);
		graph.addEdge(new Edge(a, b, RIFCSGraphProvider.RELATION_SAME_AS));

		graphService.ingestGraph(graph);

		// when obtain state
		RecordState state = graphService.getRecordState("A");

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

		Vertex i1 = new Vertex("I1", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
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
		RecordState state = graphService.getRecordState("A");

		assertThat(state.getTitle()).isEqualTo("A");

		// there should be 3 identical, A, C and I1
		assertThat(state.getIdentical().size()).isEqualTo(3);
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("A"))).isTrue();
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("C"))).isTrue();
		assertThat(state.getIdentical().stream().anyMatch(vertex -> vertex.getIdentifier().equals("I1"))).isTrue();

		// there should be 1 outbounds, (a)-[isPartOf]->(b)
		assertThat(state.getOutbounds().size()).isEqualTo(1);
		Collection<ardc.cerium.mycelium.model.Relationship> outbunds = state.getOutbounds();
		ardc.cerium.mycelium.model.Relationship aToB = outbunds.iterator().next();
		assertThat(aToB.getFrom().getIdentifier()).isEqualTo("A");
		assertThat(aToB.getTo().getIdentifier()).isEqualTo("B");
		assertThat(aToB.getRelations().get(0).getType()).isEqualTo("isPartOf");

		assertThat(state).isNotNull();
	}

}