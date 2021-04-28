package ardc.cerium.researchdata;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.researchdata.rifcs.RIFCSParser;
import ardc.cerium.researchdata.rifcs.model.Collection;
import ardc.cerium.researchdata.rifcs.model.RegistryObject;
import ardc.cerium.researchdata.rifcs.model.RegistryObjects;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RIFCSParserTest {

    @Test
	void standardParsingTest_itCanParseRelatedInfos() throws IOException {
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        RegistryObjects registryObjects = RIFCSParser.parse(rifcs);

        assertThat(registryObjects).isNotNull();
        RegistryObject ro = registryObjects.getRegistryObjects().get(0);
        assertThat(ro).isNotNull();
        assertThat(ro.getCollection()).isNotNull();

        Collection collection = ro.getCollection();
        assertThat(collection.getRelatedInfos()).isNotNull();
        assertThat(collection.getRelatedInfos().size()).isEqualTo(2);
	}

	@Test
    void itCanParseSkip() throws IOException {
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_non_group_identifiers.xml");
        RegistryObjects registryObjects = RIFCSParser.parse(rifcs);

        assertThat(registryObjects).isNotNull();
        assertThat(registryObjects.getRegistryObjects().get(0).getIdentifiers()).isNotNull();
        assertThat(registryObjects.getRegistryObjects().get(0).getIdentifiers().size()).isEqualTo(2);
    }

    @Test
	void itCanParseMultipleRegistryObjects() throws IOException {
        String rifcs = Helpers
				.readFile("src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
        RegistryObjects registryObjects = RIFCSParser.parse(rifcs);
        assertThat(registryObjects).isNotNull();
        assertThat(registryObjects.getRegistryObjects().size()).isEqualTo(2);
	}
}