package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.Neo4jTest;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RelationDocument;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.EdgeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.util.Neo4jClientBiFunctionHelper;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import({GraphService.class, VertexMapper.class, EdgeDTOMapper.class, ModelMapper.class})
class GraphServiceTest extends Neo4jTest {

	@Autowired
	GraphService graphService;

	@Autowired
	Neo4jClient neo4jClient;

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
}