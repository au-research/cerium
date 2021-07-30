package ardc.cerium.mycelium.client;

import ardc.cerium.mycelium.model.RegistryObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RDARegistryClientTest_Deprecated {

	@Disabled("Disabled registry specific content is passed in JSON payload to Mycelium")
    @Test
    void getRegistryObjectByKey() {
    	RDARegistryClient client = new RDARegistryClient("localhost");
    	RegistryObject[] registryObjects = client.getRegistryObjectByKey("C1_27");
    	assertThat(registryObjects).isNotNull();
    	assertThat(registryObjects).hasSize(1);
    }
	@Disabled("Disabled registry specific content is passed in JSON payload to Mycelium")
	@Test
	void testGetPublishedBykey() {
		RDARegistryClient client = new RDARegistryClient("localhost");
		RegistryObject registryObject = client.getPublishedByKey("C1_27");
		assertThat(registryObject).isNotNull();
		assertThat(registryObject.getRegistryObjectId()).isGreaterThanOrEqualTo(0);
		assertThat(registryObject.getClassification()).isEqualTo("collection");
	}
}