package ardc.cerium.mycelium.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAttribute;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Relation {

    @XmlAttribute
    private String type;

    private String description;

    private String url;

}
