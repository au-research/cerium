package ardc.cerium.orcid.schema.orcid.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Name {

    private String givenName = "";

    private String familyName = "";

    private String creditName;

    private String fullName;

    @JsonProperty("given-names")
    public void unpackGivenNameNested(Map<String, String> name) {
        givenName = name != null ? name.get("value") : "";
    }

    @JsonProperty("family-name")
    public void unpackFamilyNameNested(Map<String, String> name) {
        familyName = name != null ? name.get("value") : "";
    }

    @JsonProperty("credit-name")
    public void unpackCreditNameNested(Map<String, String> name) {
        creditName = name != null ? name.get("value") : null;
    }

    public String getFullName() {
        return String.format("%s %s", givenName, familyName);
    }
}
