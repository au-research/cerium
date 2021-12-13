package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DataSourceUpdatedEvent extends ApplicationEvent {

    private String dataSourceId;

    private String logMessage;

    public DataSourceUpdatedEvent(Object source, String dataSourceId, String logMessage) {
        super(source);
        this.dataSourceId = dataSourceId;
        this.logMessage = logMessage;
    }

	/**
     * Convert this object into a {@link RDAEventDTO}
	 * @return a form of {@link RDAEventDTO}
	 */
    public RDAEventDTO toRDAEventDTO() {
        RDAEventDTO dto = new RDAEventDTO();
        dto.setType("DataSourceUpdatedEvent");
        Map<String, Object> data = new HashMap<>();
        data.put("data_source_id", dataSourceId);
        data.put("log_message", logMessage);
        dto.setData(data);
        return dto;
    }
}
