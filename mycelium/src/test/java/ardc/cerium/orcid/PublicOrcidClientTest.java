package ardc.cerium.orcid;

import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PublicOrcidClientTest {

	@Test
	void testResolve() {
        PublicOrcidClient client = new PublicOrcidClient();
        OrcidRecord record = client.resolve("0000-0002-1825-0097");
        assertThat(record).isNotNull();
        assertThat(record.getPerson()).isNotNull();
        assertThat(record.getPerson().getName()).isNotNull();
        assertThat(record.getPerson().getName().getGivenName()).isEqualTo("Josiah");
        assertThat(record.getPerson().getName().getFamilyName()).isEqualTo("Carberry");
        assertThat(record.getPerson().getName().getFullName()).isEqualTo("Josiah Carberry");
	}
}