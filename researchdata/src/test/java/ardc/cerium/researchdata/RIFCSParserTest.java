package ardc.cerium.researchdata;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.researchdata.rifcs.RIFCSParser;
import ardc.cerium.researchdata.rifcs.model.Collection;
import ardc.cerium.researchdata.rifcs.model.RegistryObjects;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RIFCSParserTest {

    @Test
	void itCanParseRelatedInfos() throws IOException {
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_relatedInfos_party.xml");
        RegistryObjects registryObjects = RIFCSParser.parse(rifcs);

        assertThat(registryObjects).isNotNull();
        assertThat(registryObjects.getRegistryObject()).isNotNull();
        assertThat(registryObjects.getRegistryObject().getCollection()).isNotNull();

        Collection collection = registryObjects.getRegistryObject().getCollection();
        assertThat(collection.getRelatedInfos()).isNotNull();
        assertThat(collection.getRelatedInfos().size()).isEqualTo(2);
	}

	@Test
    void itCanParseSkip() throws IOException {
        String rifcs = Helpers.readFile("src/test/resources/rifcs/collection_non_group_identifiers.xml");
        RegistryObjects registryObjects = RIFCSParser.parse(rifcs);

        assertThat(registryObjects).isNotNull();
        assertThat(registryObjects.getRegistryObject().getIdentifiers()).isNotNull();
        assertThat(registryObjects.getRegistryObject().getIdentifiers().size()).isEqualTo(2);
    }
}