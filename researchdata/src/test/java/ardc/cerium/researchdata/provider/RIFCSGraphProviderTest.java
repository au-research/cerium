package ardc.cerium.researchdata.provider;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.researchdata.model.Edge;
import ardc.cerium.researchdata.model.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RIFCSGraphProviderTest {

    @Test
	void relatedInfos() throws IOException {

        // given a rifcs
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
        Graph graph = graphProvider.get(rifcs);

        // the graph should contains 4 nodes and 3 edges
        assertThat(graph.getVertices().size()).isEqualTo(4);
        assertThat(graph.getEdges().size()).isEqualTo(3);

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
}