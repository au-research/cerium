package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.MyceliumIntegrationTest;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GraphServiceIT extends MyceliumIntegrationTest {

    @Autowired
    GraphService graphService;

    @Autowired
    VertexRepository vertexRepository;

    @Test
	void testIngestVertex() {
        Vertex actual = new Vertex("test", RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
        graphService.ingestVertex(actual);

        assertThat(vertexRepository.existsVertexByIdentifierAndIdentifierType("test",
				RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE)).isTrue();
	}
}
