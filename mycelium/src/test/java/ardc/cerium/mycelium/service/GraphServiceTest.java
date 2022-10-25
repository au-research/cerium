package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.*;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.util.Neo4jClientBiFunctionHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.neo4j.driver.types.Relationship;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.solr.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Neo4jLabsPlugin;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataNeo4jTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Transactional(propagation = Propagation.NEVER)
@ExtendWith(SpringExtension.class)
@Import({GraphService.class, VertexMapper.class, EdgeDTOMapper.class, ModelMapper.class})
class GraphServiceTest {

	@Autowired
	GraphService graphService;

	@Autowired
	Neo4jClient neo4jClient;

	@Container
	static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
			.withLabsPlugins(Neo4jLabsPlugin.APOC);

	@AfterEach
	void wipeNeo4j() {
		neo4jClient.query("MATCH (n) DETACH DELETE n;").run();
	}

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", neo4jContainer::getAdminPassword);
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
	void ingestVertexExistingShouldUpdate() {
		// given an existing vertex
		Vertex fixture = new Vertex("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		fixture.setTitle("Old Title");
		graphService.ingestVertex(fixture);

		// when ingest a second time
		Vertex vertex = new Vertex("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		vertex.setTitle("New Title");
		graphService.ingestVertex(vertex);

		// it should have new titles
		Vertex actual = graphService.getVertexByIdentifier("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);

		assertThat(actual).isNotNull();
		assertThat(actual.getTitle()).isEqualTo("New Title");
	}

	@Test
	void deleteVertex() {
		// given an existing vertex
		Vertex fixture = new Vertex("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graphService.ingestVertex(fixture);

		// when delete
		graphService.deleteVertex(fixture);

		// it should be gone
		Vertex actual = graphService.getVertexByIdentifier("testIdentifier", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		assertThat(actual).isNull();
	}

	@Test
	void deleteUnknownVertexShouldntException() {
		graphService.deleteVertex(new Vertex("testIdentifier", "ro:key"));
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
		a.setStatus("PUBLISHED");
		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		b.setStatus("PUBLISHED");
		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus("PUBLISHED");
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
	void getDuplicateofDraftRegistryObjectTest() {
		// given A, B, C isSameAs identifier I1
		Graph graph = new Graph();
		Vertex a = new Vertex("1", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus("DRAFT");
		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.Identifier);
		Vertex c = new Vertex("2", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus("PUBLISHED");
		graph.addVertex(a, b, c);
		graph.addEdge(new Edge(a, b, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(c, b, RIFCSGraphProvider.RELATION_SAME_AS));

		graphService.ingestGraph(graph);

		// when getDuplicate of A
		Collection<Vertex> duplicates = graphService.getDuplicateRegistryObject(a);

		// there should be no duplicates only the source
		assertThat(duplicates.size()).isEqualTo(1);

		// when getDuplicate of C

		duplicates = graphService.getDuplicateRegistryObject(c);

		// there should be no duplicates
		assertThat(duplicates.size()).isEqualTo(1);

	}


	@Test
	void getDuplicateMultipleStep() {
		// given A isSameAs I1
		// B isSameAs I1 and isSameAs I2
		// C isSameAs I2
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus("PUBLISHED");
		Vertex i1 = new Vertex("I1", "local");
		i1.addLabel(Vertex.Label.Identifier);
		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		b.setStatus("PUBLISHED");
		Vertex i2 = new Vertex("I2", "local");
		i2.addLabel(Vertex.Label.Identifier);
		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus("PUBLISHED");

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
		a.setStatus("PUBLISHED");
		a.setTitle("Test Object");

		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.Identifier);

		Graph graph = new Graph();
		graph.addVertex(a, b);
		graph.addEdge(new Edge(a, b, RIFCSGraphProvider.RELATION_SAME_AS));

		graphService.ingestGraph(graph);

		// when obtain state
		// getSameAs is using apoc libraries disable testing until library can be loaded with neo4j-harness
		//RecordState state = graphService.getRecordState("A");

		// not null & has the same title
		//assertThat(state).isNotNull();
		//assertThat(state.getTitle()).isEqualTo("Test Object");
	}

	@Test
	void getRecordState() {
		// given (c)-[isSameAs]->(i1)<-[isSameAs]-(a)-[isPartOf]->(b) and
		// (b)-[hasPart]->(a)
		Vertex a = new Vertex("A", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus("PUBLISHED");
		a.setTitle("A");

		Vertex b = new Vertex("B", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		b.setStatus("PUBLISHED");
		b.setTitle("B");

		Vertex c = new Vertex("C", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus("PUBLISHED");
		c.setTitle("C");

		Vertex i1 = new Vertex("I1", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		i1.addLabel(Vertex.Label.RegistryObject);
		i1.setStatus("PUBLISHED");
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

	@Test
	void getDuplicateWithDraftsRegistryObjectTest() {
		// given A, B, C isSameAs identifier I1
		Graph graph = new Graph();
		Vertex a = new Vertex("123", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus("PUBLISHED");
		Vertex b = new Vertex("456", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		b.addLabel(Vertex.Label.RegistryObject);
		b.setStatus("DRAFT");
		Vertex i1 = new Vertex("ZZZ", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		i1.addLabel(Vertex.Label.Identifier);
		graph.addVertex(a, b, i1);

		graph.addEdge(new Edge(a, i1, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i1, RIFCSGraphProvider.RELATION_SAME_AS));


		graphService.ingestGraph(graph);

		// when getDuplicate of A
		Collection<Vertex> duplicates = graphService.getDuplicateRegistryObject(a);

		// there are NO duplicates because b is a DRAFT
		assertThat(duplicates.size()).isEqualTo(1);

		// all of them should be RegistryObject, no identifier allowed
		assertThat(duplicates.stream().allMatch(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))).isTrue();

	}

	@Test
	void testVertexCompositeMetaProperty() {
		Vertex vertex = new Vertex("123", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		vertex.setMetaAttribute("listTitle", "Random List Title");
		graphService.ingestVertex(vertex);

		// it exists in neo4j and contains the right meta value
		Vertex actual = graphService.getVertexByIdentifier("123", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		assertThat(actual).isNotNull();
		assertThat(vertex.getMetaAttribute("listTitle")).isEqualTo("Random List Title");
	}

	@Test
	void removeDanglingVertices() {
		// given a graph with a dangling vertex
		Graph graph = new Graph();
		Vertex a = new Vertex("a", "test");
		a.setId(1L);
		Vertex b = new Vertex("b", "test");
		b.setId(2L);
		Vertex c = new Vertex("c", "test");
		c.setId(3L);

		graph.addVertex(a, b, c);
		graph.addEdge(new Edge(a, b, "hasAssociationWith"));

		// when removeDangling
		graphService.removeDanglingVertices(graph);

		// the dangling vertex should be gone
		assertThat(graph.getVertices().size()).isEqualTo(2);
		assertThat(graph.getVertices().stream().filter(vertex -> {
			return vertex.getIdentifier().equals("c");
		}).findFirst().orElse(null)).isNull();
	}

	@Test
	void getRegistryObjectByKey() {
		// given (a:RegistryObject status:PUBLISHED)-[:isSameAs]->(k:Identifier {type:key})
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus(Vertex.Status.PUBLISHED);
		Vertex k = new Vertex("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graph.addVertex(a, k);
		graph.addEdge(new Edge(a, k, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// when obtaining RegistryObject from the key
		Vertex actual = graphService.getRegistryObjectByKey("key");

		// a is returned
		assertThat(actual).isNotNull();
		assertThat(actual.getIdentifier()).isEqualTo(a.getIdentifier());
		assertThat(actual.getIdentifierType()).isEqualTo(a.getIdentifierType());
	}

	@Test
	void getRegistryObjectByKeyVertex() {
		// given (a:RegistryObject status:PUBLISHED)-[:isSameAs]->(k:Identifier {type:key})
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus(Vertex.Status.PUBLISHED);
		Vertex k = new Vertex("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		k.addLabel(Vertex.Label.Identifier);
		graph.addVertex(a, k);
		graph.addEdge(new Edge(a, k, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// when obtaining RegistryObject from the keyVertex
		Vertex actual = graphService.getRegistryObjectByKeyVertex(k);

		// a is returned
		assertThat(actual).isNotNull();
		assertThat(actual.getIdentifier()).isEqualTo(a.getIdentifier());
		assertThat(actual.getIdentifierType()).isEqualTo(a.getIdentifierType());
	}

	@Test
	void getDirectInboundRelationships() {
		// given (a:Vertex)<-[:hasAssociationWith]-(b:Vertex)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		graph.addVertex(a, b);
		graph.addEdge(new Edge(b, a, "hasAssociationWith"));
		graphService.ingestGraph(graph);

		// when obtain direct inbound relationships of a
		Collection<ardc.cerium.mycelium.model.Relationship> actual = graphService.getDirectInboundRelationships(a.getIdentifier(), a.getIdentifierType());

		// we should get (b)-[hasAssociationWith]->(a)
		assertThat(actual.size()).isNotEqualTo(0);
		ardc.cerium.mycelium.model.Relationship bToA = actual.stream().filter(relationship -> {
			return relationship.getFrom().getIdentifier().equals("b") && relationship.getTo().getIdentifier().equals("a");
		}).findFirst().orElse(null);
		assertThat(bToA).isNotNull();
		assertThat(bToA.getRelations().size()).isEqualTo(1);
		assertThat(bToA.getRelations().get(0).getType()).isEqualTo("hasAssociationWith");
	}

	@Test
	void getDirectOutboundRelationships() {
		// given (a:Vertex)-[:hasAssociationWith]->(b:Vertex)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		graph.addVertex(a, b);
		graph.addEdge(new Edge(a, b, "hasAssociationWith"));
		graphService.ingestGraph(graph);

		// when obtain direct outbound relationships of a
		Collection<ardc.cerium.mycelium.model.Relationship> actual = graphService.getDirectOutboundRelationships(a.getIdentifier(), a.getIdentifierType());

		// we should get (a)-[hasAssociationWith]->(b)
		assertThat(actual.size()).isNotEqualTo(0);
		ardc.cerium.mycelium.model.Relationship aToB = actual.stream().filter(relationship -> {
			return relationship.getFrom().getIdentifier().equals("a") && relationship.getTo().getIdentifier().equals("b");
		}).findFirst().orElse(null);
		assertThat(aToB).isNotNull();
		assertThat(aToB.getRelations().size()).isEqualTo(1);
		assertThat(aToB.getRelations().get(0).getType()).isEqualTo("hasAssociationWith");
	}

	@Test
	void getAllRegistryObjects_noVertices() {
		Page<Vertex> allRegistryObjects = graphService.getAllRegistryObjects(Pageable.unpaged());
		assertThat(allRegistryObjects.getTotalElements()).isEqualTo(0L);
	}

	@Test
	void getAllRegistryObjects_noRegistryObjects() {
		// given a Vertex
		Vertex a = new Vertex("a", "doi");
		graphService.ingestVertex(a);

		Page<Vertex> allRegistryObjects = graphService.getAllRegistryObjects(Pageable.unpaged());
		assertThat(allRegistryObjects.getTotalElements()).isEqualTo(0L);
	}

	@Test
	void getAllRegistryObjects_excludesDrafts() {
		// given a Vertex
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.setStatus(Vertex.Status.DRAFT);
		graphService.ingestVertex(a);

		Page<Vertex> allRegistryObjects = graphService.getAllRegistryObjects(Pageable.unpaged());
		assertThat(allRegistryObjects.getTotalElements()).isEqualTo(0L);
	}

	@Test
	void getAllRegistryObjects() {
		// given a Vertex
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.setStatus(Vertex.Status.PUBLISHED);
		graphService.ingestVertex(a);

		Page<Vertex> allRegistryObjects = graphService.getAllRegistryObjects(Pageable.unpaged());
		assertThat(allRegistryObjects.getTotalElements()).isEqualTo(1L);
	}

	@Test
	void getLocalGraph() {
		// given a general graph
		// (a)-[:hasAssociationWith]->(b)
		// (b)-[:hasAssociationWith]->(c)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Vertex c = new Vertex("c", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		graph.addVertex(a, b, c);
		graph.addEdge(new Edge(a, b, "hasAssociationWith"));
		graph.addEdge(new Edge(b, c, "hasAssociationWith"));
		graphService.ingestGraph(graph);


		Graph actual = graphService.getLocalGraph(a);

		assertThat(actual.getVertices().size()).isGreaterThan(0);

		// localGraph of a should include (a)-[:hasAssociationWith]->(b)
		Edge aToB = actual.getEdges().stream().filter(edge -> {
			return edge.getFrom().getIdentifier().equals("a") && edge.getTo().getIdentifier().equals("b");
		}).findFirst().orElse(null);
		assertThat(aToB).isNotNull();
		assertThat(aToB.getType()).isEqualTo("hasAssociationWith");

		// but no (b)-[:hasAssociationWith]->(c)
		Edge bToC = actual.getEdges().stream().filter(edge -> {
			return edge.getFrom().getIdentifier().equals("b") && edge.getTo().getIdentifier().equals("c");
		}).findFirst().orElse(null);
		assertThat(bToC).isNull();
	}

	@Test
	void getNestedCollectionChildren() {
		// given (a)-[:hasPart]->(b) and (a)-[:hasPart]-(c) and (a)-[:hasAssociationWith]->(d)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.setObjectClass("collection");
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		b.setObjectClass("collection");
		Vertex c = new Vertex("c", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		c.setObjectClass("collection");
		Vertex d = new Vertex("d", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		d.setObjectClass("collection");
		graph.addVertex(a, b, c, d);
		graph.addEdge(new Edge(a, b, "hasPart"));
		graph.addEdge(new Edge(a, c, "hasPart"));
		graph.addEdge(new Edge(a, d, "hasAssociationWith"));
		graphService.ingestGraph(graph);

		// b and c is nested under a, that means all paths from a->b and b->c should exists, but not c->d
		Collection<ardc.cerium.mycelium.model.Relationship> actual = graphService.getNestedCollectionChildren(a, 100, 0, new ArrayList<>());
		assertThat(actual.size()).isGreaterThan(0);

		// a->b is included
		assertThat(actual.stream().anyMatch(relationship -> {
			return relationship.getFrom().getIdentifier().equals("a") && relationship.getTo().getIdentifier().equals("b");
		})).isTrue();

		// a->c is included
		assertThat(actual.stream().anyMatch(relationship -> {
			return relationship.getFrom().getIdentifier().equals("a") && relationship.getTo().getIdentifier().equals("c");
		})).isTrue();

		// a->d is not
		assertThat(actual.stream().anyMatch(relationship -> {
			return relationship.getFrom().getIdentifier().equals("a") && relationship.getTo().getIdentifier().equals("d");
		})).isFalse();
	}

	@Test
	void getNestedCollectionChildrenCount() {
		// given (a)-[:hasPart]->(b) and (a)-[:hasPart]-(c) and (a)-[:hasAssociationWith]->(d)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.setObjectClass("collection");
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		b.setObjectClass("collection");

		Vertex c = new Vertex("c", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		c.setObjectClass("collection");
		Vertex bKey = new Vertex("bKey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		Vertex cKey = new Vertex("cKey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		Vertex d = new Vertex("d", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		d.setObjectClass("collection");
		graph.addVertex(a, b, c, d, bKey, cKey);
		graph.addEdge(new Edge(a, bKey, "hasPart"));
		graph.addEdge(new Edge(a, cKey, "hasPart"));
		graph.addEdge(new Edge(b, bKey, "isSameAs"));
		graph.addEdge(new Edge(c, cKey, "isSameAs"));
		graph.addEdge(new Edge(a, d, "hasAssociationWith"));
		graphService.ingestGraph(graph);

		// a has exactly 2 nested children
		int actual = graphService.getNestedCollectionChildrenCount(a.getIdentifier(), new ArrayList<>());
		assertThat(actual).isEqualTo(2);
	}

	@Test
	void getSameAs() {
		// given (a Published)-[isSameAs]->(i)<-[isSameAs]-(b)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.addLabel(Vertex.Label.RegistryObject);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Vertex i = new Vertex("i", "local");
		graph.addVertex(a, b, i);
		graph.addEdge(new Edge(a, i, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// a, b, i turns up
		Collection<Vertex> actual = graphService.getSameAs(a);
		assertThat(actual.size()).isEqualTo(3);
	}

	@Test
	void getSameAsWithIdentifierAndType_callsGetSameAs() {
		GraphService mockedService = Mockito.mock(GraphService.class);
		doCallRealMethod().when(mockedService).getSameAs(any(String.class), any(String.class));
		when(mockedService.getVertexByIdentifier(any(String.class), any(String.class))).thenReturn(new Vertex());
		mockedService.getSameAs("identifier", "type");
		verify(mockedService, times(1)).getVertexByIdentifier(any(String.class), any(String.class));
		verify(mockedService, times(1)).getSameAs(any(Vertex.class));

		// null cases
		assertThat(mockedService.getSameAs("identifier", null).size()).isEqualTo(0);
		assertThat(mockedService.getSameAs("identifier", "").size()).isEqualTo(0);
		assertThat(mockedService.getSameAs("", "").size()).isEqualTo(0);
		assertThat(mockedService.getSameAs("", "type").size()).isEqualTo(0);
		assertThat(mockedService.getSameAs(null, "type").size()).isEqualTo(0);
	}

	@Test
	void getSameAsRegistryObject_Published() {
		// given (a Published)-[isSameAs]->(i)<-[isSameAs]-(b Published)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.addLabel(Vertex.Label.RegistryObject);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		b.addLabel(Vertex.Label.RegistryObject);
		Vertex i = new Vertex("i", "local");
		graph.addVertex(a, b, i);
		graph.addEdge(new Edge(a, i, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(b, i, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// when getSameAsRegistryObject of a, b turns up
		Collection<Vertex> actual = graphService.getSameAsRegistryObject(a);
		assertThat(actual.stream().anyMatch(vertex -> {
			return vertex.getIdentifier().equals("b");
		})).isTrue();
	}

	@Test
	void getSameAsIdentifierWithType() {
		GraphService mockedService = Mockito.mock(GraphService.class);
		doCallRealMethod().when(mockedService).getSameAsIdentifierWithType(any(Vertex.class), any(String.class));
		when(mockedService.getSameAs(any(String.class), any(String.class))).thenReturn(Collections.emptyList());
		mockedService.getSameAsIdentifierWithType(new Vertex("id", "type"), "type");
		verify(mockedService, times(1)).getSameAs(any(String.class), any(String.class));
	}

	@Test
	void getSameAsIdentifierWithTypeString() {
		GraphService mockedService = Mockito.mock(GraphService.class);
		doCallRealMethod().when(mockedService).getSameAsIdentifierWithType(any(String.class), any(String.class), any(String.class));
		when(mockedService.getSameAs(any(String.class), any(String.class))).thenReturn(Collections.emptyList());
		mockedService.getSameAsIdentifierWithType("identifier", "identifier-type", "type");
		verify(mockedService, times(1)).getSameAs(any(String.class), any(String.class));
	}

	@Test
	void streamChildCollection() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamChildCollection(new Vertex("test", "test"));
		verify(mockedRepository, times(1)).streamSpanningTreeFromId("test", "isSameAs|hasPart>|outputs>|hasOutput>|isFunderOf>", "collection");
	}

	@Test
	void streamChildActivity() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamChildActivity(new Vertex("test", "test"));
		verify(mockedRepository, times(1)).streamSpanningTreeFromId("test", "isSameAs|hasPart>", "activity");
	}

	@Test
	void streamParentParty() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamParentParty(new Vertex("test", "test"));
		verify(mockedRepository, times(1)).streamSpanningTreeFromId("test", "isSameAs|isPartOf>|isOutputOf>|isFundedBy>", "party");
	}

	@Test
	void streamParentActivity() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamParentActivity(new Vertex("test", "test"));
		verify(mockedRepository, times(1)).streamSpanningTreeFromId("test", "isSameAs|isPartOf>", "activity");
	}

	@Test
	void getVertexIdentifiersByType() {
		// given 2 vertex with type doi and 1 vertex with type RegistryObject
		graphService.ingestVertex(new Vertex("a", "doi"));
		graphService.ingestVertex(new Vertex("b", "doi"));
		graphService.ingestVertex(new Vertex("c", "ro:id"));

		// when getVertexIdentifiersByType doi, 2 returns
		Collection<String> actualDOIs = graphService.getVertexIdentifiersByType("doi");
		assertThat(actualDOIs.size()).isEqualTo(2);
		assertThat(actualDOIs.contains("a")).isTrue();
		assertThat(actualDOIs.contains("b")).isTrue();

		// when getVertexIdentifiersByType ro:id, 1 returns
		Collection<String> actualRO = graphService.getVertexIdentifiersByType("ro:id");
		assertThat(actualRO.size()).isEqualTo(1);
		assertThat(actualRO.contains("c")).isTrue();
	}

	@Test
	void streamParentCollection() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamParentCollection(new Vertex("test", "test"));
		verify(mockedRepository, times(1)).streamSpanningTreeFromId("test", "isSameAs|isPartOf>", "collection");
	}

	@Test
	void getRegistryObjectGraph() {
		// todo
	}

	@Test
	void getGrantsNetworkDownwards() {
		// given (p party)-[isFunderOf]->(a activity)-[hasOutput]->(c collection)
		Graph graph = new Graph();
		Vertex p = new Vertex("p", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		p.addLabel(Vertex.Label.RegistryObject);
		p.setStatus(Vertex.Status.PUBLISHED);
		Vertex pkey = new Vertex("pkey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus(Vertex.Status.PUBLISHED);
		Vertex akey = new Vertex("akey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex c = new Vertex("c", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus(Vertex.Status.PUBLISHED);
		Vertex ckey = new Vertex("ckey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graph.addVertex(p, pkey, a, akey, c, ckey);
		graph.addEdge(new Edge(p, pkey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(a, akey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(c, ckey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(p, akey, "isFunderOf"));
		graph.addEdge(new Edge(a, ckey, "hasOutput"));
		graphService.ingestGraph(graph);

		Graph actual = graphService.getGrantsNetworkDownwards(p, new ArrayList<>());

		// 3 vertices from p -> includes p, a and c
		assertThat(actual.getVertices().size()).isEqualTo(3);
		assertThat(actual.getVertices().stream().map(vertex -> {
			return vertex.getIdentifier();
		}).collect(Collectors.toList()).containsAll(Arrays.asList("p", "a", "c"))).isTrue();

		// 2 edges, isFunderOf and hasOutput
		assertThat(actual.getEdges().size()).isEqualTo(2);
		assertThat(actual.getEdges().stream().map(edge -> {
			return edge.getType();
		}).collect(Collectors.toList()).containsAll(Arrays.asList("isFunderOf", "hasOutput"))).isTrue();
	}

	@Test
	void getGrantsNetworkGraphUpwards() {
		// given (p party)<-[isFundedBy]-(a activity)<-[isOutputOf]-(c collection)
		Graph graph = new Graph();
		Vertex p = new Vertex("p", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		p.addLabel(Vertex.Label.RegistryObject);
		p.setStatus(Vertex.Status.PUBLISHED);
		Vertex pkey = new Vertex("pkey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.addLabel(Vertex.Label.RegistryObject);
		a.setStatus(Vertex.Status.PUBLISHED);
		Vertex akey = new Vertex("akey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex c = new Vertex("c", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		c.addLabel(Vertex.Label.RegistryObject);
		c.setStatus(Vertex.Status.PUBLISHED);
		Vertex ckey = new Vertex("ckey", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graph.addVertex(p, pkey, a, akey, c, ckey);
		graph.addEdge(new Edge(p, pkey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(a, akey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(c, ckey, RIFCSGraphProvider.RELATION_SAME_AS));
		graph.addEdge(new Edge(a, pkey, "isFundedBy"));
		graph.addEdge(new Edge(c, akey, "isOutputOf"));
		graphService.ingestGraph(graph);

		Graph actual = graphService.getGrantsNetworkGraphUpwards(c, new ArrayList<>());

		// 3 vertices from p -> includes p, a and c
		assertThat(actual.getVertices().size()).isEqualTo(3);
		assertThat(actual.getVertices().stream().map(vertex -> {
			return vertex.getIdentifier();
		}).collect(Collectors.toList()).containsAll(Arrays.asList("p", "a", "c"))).isTrue();

		// 2 edges, isFundedBy and isOutputOf
		assertThat(actual.getEdges().size()).isEqualTo(2);
		assertThat(actual.getEdges().stream().map(edge -> {
			return edge.getType();
		}).collect(Collectors.toList()).containsAll(Arrays.asList("isFundedBy", "isOutputOf"))).isTrue();
	}

	@Test
	void collapseGraph() {
		// given a graph of (a)-[isSameAs]->(k) and (b)-[hasAssociationWith]->(k)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		a.setId(1L);
		Vertex k = new Vertex("k", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		k.setId(2L);
		Vertex b = new Vertex("b", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		b.setId(3L);
		graph.addVertex(a, b, k);
		graph.addEdge(new Edge(a, k, RIFCSGraphProvider.RELATION_SAME_AS, 1L));
		graph.addEdge(new Edge(b, k, "hasAssociationWith", 2L));

		// when collapse
		Graph actual = graphService.collapseGraph(graph);

		// (b)-[hasAssociationWith]->(a) is found, only 2 vertices
		assertThat(actual.getVertices().size()).isEqualTo(2);
		assertThat(actual.getEdges().stream().anyMatch(edge -> {
			return edge.getFrom().getIdentifier().equals("b") && edge.getTo().getIdentifier().equals("a") && edge.getType().equals("hasAssociationWith");
		})).isTrue();
	}

	@Test
	void getGraphBetweenVertices() {
		// todo
	}

	@Test
	void getRelationTypeGrouping() {
		// given (a)-[hasAssociationWith]->(b collection), (a)-[hasAssociationWith]->(c collection)
		// (a)-[isOwnedBy]->(p party)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", "ro:id");
		Vertex b = new Vertex("b", "ro:id");
		b.setObjectClass("collection");
		b.setObjectType("collection");
		Vertex c = new Vertex("c", "ro:id");
		c.setObjectClass("collection");
		c.setObjectType("collection");
		Vertex p = new Vertex("p", "ro:id");
		p.setObjectClass("party");
		p.setObjectType("group");
		graph.addVertex(a, b, c, p);
		graph.addEdge(new Edge(a, b, "hasAssociationWith"));
		graph.addEdge(new Edge(a, c, "hasAssociationWith"));
		graph.addEdge(new Edge(a, p, "isOwnedBy"));
		graphService.ingestGraph(graph);

		// when get RelationType Grouping of (a)
		Collection<RelationTypeGroup> actual = graphService.getRelationTypeGrouping(a);

		// (a) hasAssociationWith 2 collections
		assertThat(actual.stream().anyMatch(relationTypeGroup -> {
			return relationTypeGroup.getRelation().equals("hasAssociationWith") && relationTypeGroup.getCount() == 2 && relationTypeGroup.getObjectClass().equals("collection");
		})).isTrue();

		// (a) isOwnedBy 1 party
		assertThat(actual.stream().anyMatch(relationTypeGroup -> {
			return relationTypeGroup.getRelation().equals("isOwnedBy") && relationTypeGroup.getCount() == 1 && relationTypeGroup.getObjectClass().equals("party");
		})).isTrue();
	}

	@Test
	void deletePrimaryKeyEdge() {
		// given (a)-[hasAssociationWith]->(b), (a)-[hasAssociationWith origin:PrimaryLink]->(c ro:key)
		Graph graph = new Graph();
		Vertex a = new Vertex("a", "ro:id");
		Vertex b = new Vertex("b", "ro:id");
		Vertex c = new Vertex("c", "ro:key");
		graph.addVertex(a, b , c);
		Edge pkEdge = new Edge(a, c, "hasAssociationWith");
		pkEdge.setOrigin(RIFCSGraphProvider.ORIGIN_PRIMARY_LINK);
		graph.addEdge(pkEdge);
		graph.addEdge(new Edge(a, b, "hasAssociationWith"));
		graphService.ingestGraph(graph);

		// when deletePrimaryKeyEdge
		graphService.deletePrimaryKeyEdge("c");

		// no edge primary key edge remains
		Collection<ardc.cerium.mycelium.model.Relationship> actual = graphService.getDirectOutboundRelationships("a", "ro:id");
		assertThat(actual.size()).isEqualTo(1);
		assertThat(actual.stream().anyMatch(relationship -> {
			return relationship.getRelations().stream().anyMatch(edgeDTO -> {
				return edgeDTO.getOrigin().equals(RIFCSGraphProvider.ORIGIN_PRIMARY_LINK);
			});
		})).isFalse();
	}

	@Test
	void getNestedCollectionParents() {
		GraphService mockedService = Mockito.mock(GraphService.class);
		doCallRealMethod().when(mockedService).getNestedCollectionParents(any(Vertex.class));
		Vertex fixture = new Vertex();
		fixture.setStatus("PUBLISHED");
		ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
		mockedService.getNestedCollectionParents(fixture);
		verify(mockedService, times(1)).getGraphsFromPaths(queryCaptor.capture());
		assertThat(queryCaptor.getValue().contains("labelFilter: '-DRAFT'"));
	}

	@Test
	void setRegistryObjectKeyNodeTerminated() {
		// there exists a lonely key vertex and a key with a real RegistryObject
		Graph graph = new Graph();
		Vertex k = new Vertex("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		Vertex a = new Vertex("1", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.addLabel(Vertex.Label.RegistryObject);
		Vertex k2 = new Vertex("k2", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graph.addVertex(a, k, k2);
		graph.addEdge(new Edge(a, k2, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// when setRegistryObjectKeyNodeTerminated
		graphService.setRegistryObjectKeyNodeTerminated();

		// the key vertex is set to Terminated
		Vertex actual = graphService.getVertexByIdentifier("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		assertThat(actual).isNotNull();
		assertThat(actual.getLabels().contains("Terminated")).isTrue();
	}

	@Test
	void reinstateTerminatedNodes() {
		// given a Terminated key node with a real Object
		Graph graph = new Graph();
		Vertex k = new Vertex("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		graph.addVertex(k);
		graphService.ingestGraph(graph);
		graphService.setRegistryObjectKeyNodeTerminated();

		Vertex actual = graphService.getVertexByIdentifier("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		assertThat(actual).isNotNull();
		assertThat(actual.getLabels().contains("Terminated")).isTrue();

		Vertex a = new Vertex("1", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE, Vertex.Status.PUBLISHED);
		a.addLabel(Vertex.Label.RegistryObject);
		graph.addVertex(a, k);
		graph.addEdge(new Edge(a, k, RIFCSGraphProvider.RELATION_SAME_AS));
		graphService.ingestGraph(graph);

		// when reinstate
		graphService.reinstateTerminatedNodes();

		// it doesn't have the Terminated label anymore
		actual = graphService.getVertexByIdentifier("key", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		assertThat(actual).isNotNull();
		assertThat(actual.getLabels().contains("Terminated")).isFalse();
	}

	@Test
	void streamRegistryObjectFromDataSource() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamRegistryObjectFromDataSource("1");
		verify(mockedRepository, times(1)).streamRegistryObjectByDataSourceId("1");
	}

	@Test
	void streamRegistryObjectFromDataSourceIdAndClass() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamRegistryObjectFromDataSource("1", "collection");
		verify(mockedRepository, times(1)).streamRegistryObjectByDataSourceAndClass("1", "collection");
	}

	@Test
	void streamVertexByIdentifierType() {
		VertexRepository mockedRepository = Mockito.mock(VertexRepository.class);
		GraphService mockedGraphService = new GraphService(mockedRepository, graphService.getNeo4jClient(), graphService.getVertexMapper(), graphService.getEdgeDTOMapper());
		mockedGraphService.streamVertexByIdentifierType("type");
		verify(mockedRepository, times(1)).streamVertexByIdentifierType("type");
	}

}