package ardc.cerium.doi.schema.citeproc.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CiteProcJson {
    private String title;

    private String publisher;

    @JsonProperty(value="DOI")
    private String doi;

    @JsonProperty(value="URL")
    private String url;

    private String type;

    private String source;
}
