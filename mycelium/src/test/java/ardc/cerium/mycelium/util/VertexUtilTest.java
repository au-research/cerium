package ardc.cerium.mycelium.util;

import ardc.cerium.mycelium.model.Vertex;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class VertexUtilTest {

	@Test
	void testResolveDOIVertex() {
        // given a vertex with empty title
        Vertex vertex = new Vertex("10.1126/science.169.3946.635", "doi");
        assertThat(vertex.getTitle()).isNull();

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is updated to the right value fetched from DOI server
        assertThat(vertex.getTitle()).isEqualTo("The Structure of Ordinary Water");
	}

    @Test
    void testResolveUnknownDOIVertex() {
        // given a vertex with pretty much unresolvable value
        Vertex vertex = new Vertex("10.xxx/very-much-not-found", "doi");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is not changed, and there's no error
        assertThat(vertex.getTitle()).isNull();
    }

    @Test
	void testResolveORCIDVertex() {
        // given an orcid vertex with an empty title
        Vertex vertex = new Vertex("0000-0002-1825-0097", "orcid");
        assertThat(vertex.getTitle()).isNull();

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is updated to the right value fetched from ORCID server
        assertThat(vertex.getTitle()).isEqualTo("Josiah Carberry");
	}

    @Test
    void testResolveUnknownORCIDVertex() {
        // given a vertex with pretty much unresolvable value
        Vertex vertex = new Vertex("not-valid", "orcid");

        // upon resolving
        VertexUtil.resolveVertex(vertex);

        // the title is not changed, and there's no error
        assertThat(vertex.getTitle()).isNull();
    }


}