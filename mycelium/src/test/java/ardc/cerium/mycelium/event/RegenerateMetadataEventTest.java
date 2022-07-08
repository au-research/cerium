package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RegenerateMetadataEventTest {

	@Test
	void toRDAEventDTO() {
		RegenerateMetadataEvent event = new RegenerateMetadataEvent(this, "1");
		assertThat(event.getRegistryObjectId()).isEqualTo("1");
		event.setDci(true);

		RDAEventDTO dto1 = event.toRDAEventDTO();
		assertThat(dto1.getType()).isEqualTo("RegenerateMetadataEvent");
		assertThat(dto1.getData().get("registryObjectId")).isEqualTo("1");
		assertThat(dto1.getData().get("dci")).isEqualTo(true);
		assertThat(dto1.getData().get("scholix")).isNull();

		event.setDci(false);
		event.setScholix(true);

		RDAEventDTO dto2 = event.toRDAEventDTO();
		assertThat(dto2.getType()).isEqualTo("RegenerateMetadataEvent");
		assertThat(dto2.getData().get("registryObjectId")).isEqualTo("1");
		assertThat(dto2.getData().get("dci")).isNull();
		assertThat(dto2.getData().get("scholix")).isEqualTo(true);

	}

}