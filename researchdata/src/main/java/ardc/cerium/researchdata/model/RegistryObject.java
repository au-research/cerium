package ardc.cerium.researchdata.model;

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
}
