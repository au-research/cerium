package ardc.cerium.mycelium.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAttribute;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Identifier {

    @XmlAttribute
    private String type;

    @JacksonXmlText
    private String value;
}
