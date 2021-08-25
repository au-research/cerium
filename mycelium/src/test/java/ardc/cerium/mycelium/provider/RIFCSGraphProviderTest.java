package ardc.cerium.mycelium.provider;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.RelationLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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

        // given a json payload
        String json = Helpers.readFile("src/test/resources/653061.json");
        RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
        ObjectMapper mapper = new ObjectMapper();
        RegistryObject ro = mapper.readValue(json, RegistryObject.class);
        Graph graph = graphProvider.get(ro);

        // the graph should contains 5 nodes and 4 edges
        assertThat(graph.getVertices().size()).isEqualTo(5);

        // contains 4 explicit edge
        assertThat(graph.getEdges().stream().filter(edge -> !edge.isReverse()).count()).isEqualTo(4);

        // 2 reversed edge
        assertThat(graph.getEdges().stream().filter(Edge::isReverse).count()).isEqualTo(2);

        // 3 ro:key node
        assertThat(graph.getVertices().stream()
				.filter(node -> node.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).count())
						.isEqualTo(3);

        // 1 local identifier node with 1 isSameAs going to that node
        assertThat(graph.getVertices().stream()
				.filter(node -> node.getIdentifierType().equals("ro:key")
						&& node.getIdentifier().equals("C1997_23"))
				.count()).isEqualTo(1);
	}

    @Test
    @DisplayName("Obtaining Reversed Edge if possible")
	void getReversedEdge() {
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