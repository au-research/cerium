package ardc.cerium.mycelium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RegistryObject {

    @JsonProperty("registry_object_id")
    private Long registryObjectId;

    private String status;

    private String key;

    private String type;

    @JsonProperty("class")
    private String classification;

    private String rifcs;
}
