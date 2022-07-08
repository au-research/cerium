package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PortalIndexUpdateEventTest {

	@Test
	void toRDAEventDTO() {
		PortalIndexUpdateEvent event = new PortalIndexUpdateEvent(this, "1", "to_title", "*", "new title",
				"relationType");
		RDAEventDTO rdaEventDTO = event.toRDAEventDTO();
		assertThat(rdaEventDTO.getType()).isEqualTo("PortalIndexUpdateEvent");
		Map<String, Object> data = rdaEventDTO.getData();
		assertThat(data.get("registry_object_id")).isEqualTo("1");
		assertThat(data.get("indexed_field")).isEqualTo("to_title");
		assertThat(data.get("search_value")).isEqualTo("*");
		assertThat(data.get("new_value")).isEqualTo("new title");
		assertThat(data.get("relationship_type")).isEqualTo("relationType");
	}

}