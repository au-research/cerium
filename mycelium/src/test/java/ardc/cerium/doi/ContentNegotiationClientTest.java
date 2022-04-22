package ardc.cerium.doi;

import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.doi.schema.rdf.RDF;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ContentNegotiationClientTest {

	@Test
	void itReturnsRDF() {
        ContentNegotiationClient client = new ContentNegotiationClient();
        RDF rdf= client.resolveRDF("10.1126/science.169.3946.635");
        assertThat(rdf).isNotNull();
        assertThat(rdf.getDescription().getTitle()).isEqualTo("The Structure of Ordinary Water");
        System.out.println(rdf);
	}

    @Test
	void itReturnsCiteprocJson() {
        ContentNegotiationClient client = new ContentNegotiationClient();
        CiteProcJson json = client.resolveCiteProcJson("10.1126/science.169.3946.635");
        assertThat(json).isNotNull();
        assertThat(json.getTitle()).isEqualTo("The Structure of Ordinary Water");
	}
}