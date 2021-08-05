package ardc.cerium.mycelium.util;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import ardc.cerium.mycelium.rifcs.model.RegistryObjects;
import ardc.cerium.mycelium.util.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TestHelperTest {

    @Test
    void test_jsonpayload_creation() throws IOException {
        String xml = Helpers.readFile("src/test/resources/scenarios/15_RelationshipScenario/15_RelationshipScenario.xml");
        List<String> packages = TestHelper.buildJsonPackages(xml);
        assertThat(packages.size()).isEqualTo(8);
        ObjectMapper mapper = new ObjectMapper();

        String jsonPayload = packages.get(0);
        RegistryObject ro = mapper.readValue(jsonPayload, RegistryObject.class);

        RegistryObjects registryObjects = RIFCSParser.parse(String.format("<RegistryObjects>%s</RegistryObjects>",
                    new String(Base64.getDecoder().decode(ro.getRifcs()))));
        assert registryObjects != null;
        List<ardc.cerium.mycelium.rifcs.model.RegistryObject> ros = registryObjects.getRegistryObjects();
        assertThat(ros.get(0).getKey()).isEqualTo("C1_15");
        jsonPayload = packages.get(5);
        ro = mapper.readValue(jsonPayload, RegistryObject.class);
        assertThat(ro.getKey()).isEqualTo("S1_15");
        assertThat(ro.getType()).isEqualTo("software");
    }

}