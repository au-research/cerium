package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PortalIndexUpdateEvent extends ApplicationEvent {

    private String registryObjectId;

    private String indexedField;

    private String searchValue;

    private String newValue;

    private String relationshipType;

    public PortalIndexUpdateEvent(Object source, String registryObjectId, String indexedField,
                                  String searchValue, String newValue, String relationshipType) {
        super(source);
        this.registryObjectId = registryObjectId;
        this.indexedField = indexedField;
        this.searchValue = searchValue;
        this.newValue = newValue;
        this.relationshipType = relationshipType;
    }

    /**
     * Convert this object into a {@link RDAEventDTO}
     * @return a form of {@link RDAEventDTO}
     */
    public RDAEventDTO toRDAEventDTO() {
        RDAEventDTO dto = new RDAEventDTO();
        dto.setType("PortalIndexUpdateEvent");
        Map<String, Object> data = new HashMap<>();
        data.put("registry_object_id", this.registryObjectId);
        data.put("indexed_field", this.indexedField);
        data.put("search_value", this.searchValue);
        data.put("new_value", this.newValue);
        data.put("relationship_type", this.relationshipType);
        dto.setData(data);
        return dto;
    }
}
