package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.Neo4jTest;
import ardc.cerium.researchdata.model.Edge;
import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.RelationDocument;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.provider.RIFCSGraphProvider;
import ardc.cerium.researchdata.util.Neo4jClientBiFunctionHelper;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.internal.value.PathValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import(GraphService.class)
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
		RelationDocument result = neo4jClient
				.query("MATCH (n:Vertex {identifier: $fromID, identifierType: $fromIDType})-[r]->() RETURN r")
				.bind("fromNode").to("fromID")
				.bind(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE).to("fromIDType")
				.fetchAs(RelationDocument.class)
				.mappedBy((typeSystem, record) -> Neo4jClientBiFunctionHelper.toRelationDocument(record, "r"))
				.one().orElse(null);
		assertThat(result).isNotNull();
		assertThat(result.getRelationType()).isEqualTo("isRelatedTo");
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