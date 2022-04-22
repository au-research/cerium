package ardc.cerium.orcid.schema.orcid.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrcidIdentifier {
    private String uri;
    private String path;
    private String host;
}
