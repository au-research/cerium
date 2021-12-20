package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.MyceliumIntegrationTest;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GraphServiceIT extends MyceliumIntegrationTest {

    @Autowired
    GraphService graphService;

    @Autowired
    VertexRepository vertexRepository;

    @Autowired
    Neo4jClient neo4jClient;

    @Test
	void testIngestVertex() {
        Vertex actual = new Vertex("test", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
        graphService.ingestVertex(actual);

        assertThat(vertexRepository.existsVertexByIdentifierAndIdentifierType("test",
				RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).isTrue();
	}

    @Test
	void uponBootItshouldHaveIndices() {
        Collection<String> indicesName = neo4jClient.query("CALL db.indexes() YIELD name").fetchAs(String.class).mappedBy(((typeSystem, record) -> {
            return record.get("name").asString();
        })).all();
        assertThat(indicesName.contains("vertex_id")).isTrue();
        assertThat(indicesName.contains("ro_id")).isTrue();
        assertThat(indicesName.contains("vertex_type")).isTrue();
        assertThat(indicesName.contains("ro_class")).isTrue();
    }
}
