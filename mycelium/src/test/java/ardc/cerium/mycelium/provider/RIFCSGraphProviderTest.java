package ardc.cerium.mycelium.provider;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.client.RDARegistryClient;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.RelationLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RIFCSGraphProviderTest {

    @BeforeEach
	void setUp() throws IOException {
        RelationLookupService lookupService = new RelationLookupService();
        lookupService.loadLookupTable();
	}

    @Test
    @DisplayName("Loads a rifcs with relatedInfo should provides all relevant Vertices and Edges")
	void relatedInfos() throws IOException {

        // given a rifcs
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        RIFCSGraphProvider graphProvider = new RIFCSGraphProvider(new RDARegistryClient("localhost"));
        Graph graph = graphProvider.get(rifcs);

        // the graph should contains 4 nodes and 3 edges
        assertThat(graph.getVertices().size()).isEqualTo(4);

        // contains 3 explicit edge
        assertThat(graph.getEdges().stream().filter(edge -> !edge.isReverse()).count()).isEqualTo(3);

        // 2 reversed edge
        assertThat(graph.getEdges().stream().filter(Edge::isReverse).count()).isEqualTo(2);

        // 1 ro:key node
        assertThat(graph.getVertices().stream()
				.filter(node -> node.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).count())
						.isEqualTo(1);

        // 1 local identifier node with 1 isSameAs going to that node
        assertThat(graph.getVertices().stream()
				.filter(node -> node.getIdentifierType().equals("local")
						&& node.getIdentifier().equals("AODN:a439fbc6-9150-470b-a8db-1a8fd4DCIdasdf336d2AUT"))
				.count()).isEqualTo(1);

        // 1 isSameAs edge going between the origin node and the local identifier node
        Edge isSameAsEdge = graph.getEdges().stream().filter(relation -> relation.getType().equals(RIFCSGraphProvider.RELATION_SAME_AS)).findAny().orElse(null);
        assertThat(isSameAsEdge).isNotNull();
        assertThat(isSameAsEdge.getFrom().getIdentifier()).isEqualTo("AUTestingRecords2DCIRecords6");
        assertThat(isSameAsEdge.getTo().getIdentifier())
				.isEqualTo("AODN:a439fbc6-9150-470b-a8db-1a8fd4DCIdasdf336d2AUT");
	}

    @Test
    @DisplayName("Obtaining Reversed Edge is possible")
	void getReversedEdge() {
        RIFCSGraphProvider graphProvider = new RIFCSGraphProvider(new RDARegistryClient("localhost"));

        Vertex a = new Vertex("a", "ro:key");
        Vertex b = new Vertex("b", "ro:key");
        Edge aTob = new Edge(a, b, "isPartOf");

        Edge bToA = RIFCSGraphProvider.getReversedEdge(aTob);
        assertThat(bToA).isNotNull();
        assertThat(bToA).isInstanceOf(Edge.class);
        assertThat(bToA.getFrom()).isEqualTo(b);
        assertThat(bToA.getTo()).isEqualTo(a);
        assertThat(bToA.isReverse()).isTrue();
        assertThat(bToA.getType()).isEqualTo("hasPart");
	}
}