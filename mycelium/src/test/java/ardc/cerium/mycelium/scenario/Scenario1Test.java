package ardc.cerium.mycelium.scenario;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class Scenario1Test {

	@Test
	void scenario1_graphGeneration() throws IOException {
		String rifcs = Helpers
				.readFile("src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
		Graph graph = (new RIFCSGraphProvider()).get(rifcs);

		// there are 5 vertices with 2 being the registryObjects
		List<Vertex> vertices = graph.getVertices();
		assertThat(vertices.size()).isEqualTo(5);
		assertThat(vertices.stream()
				.filter(vertex -> vertex.getLabels().contains(Vertex.Label.RegistryObject.toString())).count())
						.isEqualTo(2);

		// there are 4 edges with 1 being the hasCollector connecting 2 registryObject (not counting reversed)
		List<Edge> edges = graph.getEdges();
		assertThat(edges.stream().filter(edge -> !edge.isReverse()).count()).isEqualTo(4);
		assertThat(edges.stream().filter(edge -> edge.getType().equals("hasCollector") && !edge.isReverse()).count()).isEqualTo(1);
	}

	// todo scenario1_integration - test the data gets into neo4j, SOLR and available for searching
}
