package ardc.cerium.orcid.schema.orcid.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrcidRecord {
    private String orcidIdentifier;
    private Person person;
}
