package ardc.cerium.mycelium.model.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.junit.jupiter.api.Assertions.*;

class RDAEventDTOTest {

    @Test
	void test_construction() {
        RDAEventDTO dto = new RDAEventDTO();
        assertThat(dto.getId()).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	}
}