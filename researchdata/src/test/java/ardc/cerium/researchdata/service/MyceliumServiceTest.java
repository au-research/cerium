package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.Neo4jTest;
import ardc.cerium.researchdata.model.Edge;
import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Import({ MyceliumService.class, GraphService.class })
class MyceliumServiceTest extends Neo4jTest {

	@Autowired
	MyceliumService myceliumService;

	@Autowired
	GraphService graphService;

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

		// (a)-[:isSameAs]->(i1)<-[:isSameAs]-(c)-[:isSameAs]->(i2)<-[:isSameAs]-(c)
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

}