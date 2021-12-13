package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class RDAEventDTO {

    private String id;

    private String type;

    private Map<String, Object> data;

    public RDAEventDTO() {
        this.setId(UUID.randomUUID().toString());
    }
}
