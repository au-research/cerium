package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RegenerateMetadataEvent extends ApplicationEvent {

    private String registryObjectId;
    private boolean dci = false;
    private boolean scholix = false;

    public RegenerateMetadataEvent(Object source, String registryObjectId) {
        super(source);
        this.registryObjectId = registryObjectId;
    }

    public RDAEventDTO toRDAEventDTO() {
        RDAEventDTO dto = new RDAEventDTO();
        dto.setType("RegenerateMetadataEvent");
        Map<String, Object> data = new HashMap<>();
        data.put("registryObjectId", registryObjectId);
        if (dci) {
            data.put("dci", true);
        }
        if (scholix) {
            data.put("scholix", true);
        }
        dto.setData(data);
        return dto;
    }
}
