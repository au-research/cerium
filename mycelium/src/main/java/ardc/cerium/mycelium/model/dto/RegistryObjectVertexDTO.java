package ardc.cerium.mycelium.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class RegistryObjectVertexDTO {

    private String registryObjectId;

    private String objectType;

    private String objectClass;

    private String url;

    private String dataSourceId;

    private Date createdAt;

    private Date updatedAt;

    private String status;

    private Map<String, Object> meta;

}
