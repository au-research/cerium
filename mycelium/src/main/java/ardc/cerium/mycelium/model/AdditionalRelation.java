package ardc.cerium.mycelium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class AdditionalRelation {
    private String origin;
    private String toKey;
    private String relationType;
}
