package ardc.cerium.ror.schema.ror.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RorRecord {
    private String id;
    private String name;


    @JsonProperty("name")
    public String getName() {
        return String.format("%s", name);
    }

    @JsonProperty("id")
    public String getId() {
        return String.format("%s", id);
    }
}
