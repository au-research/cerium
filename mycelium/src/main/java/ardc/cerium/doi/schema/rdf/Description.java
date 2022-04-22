package ardc.cerium.doi.schema.rdf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Description {

    @JacksonXmlProperty(isAttribute = true)
    private String about;

    private String title;

    private String identifier;

    private String publisher;

    private String doi;
}
