package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DataSourceUpdatedEventTest {

    @Test
	void test_creation() {
        DataSourceUpdatedEvent event = new DataSourceUpdatedEvent(this, "1", "logMessage");
        assertThat(event.getDataSourceId()).isEqualTo("1");
        assertThat(event.getLogMessage()).isEqualTo("logMessage");
	}

    @Test
	void test_toRDAEventDTO() {
        DataSourceUpdatedEvent event = new DataSourceUpdatedEvent(this, "1", "logMessage");
        RDAEventDTO rdaEventDTO = event.toRDAEventDTO();
        assertThat(rdaEventDTO.getType()).isEqualTo("DataSourceUpdatedEvent");
        assertThat(rdaEventDTO.getData().get("data_source_id")).isEqualTo("1");
        assertThat(rdaEventDTO.getData().get("log_message")).isEqualTo("logMessage");
    }
}