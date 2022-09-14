package ardc.cerium.mycelium.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAttribute;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Relation {

    @XmlAttribute
    private String type;

    @JacksonXmlProperty(localName = "description")
    @JacksonXmlElementWrapper(useWrapping = false)
    private String description;

    private String url;


    /**
     * relations can have multiple descriptions
     * we just concatenate them here
     * @param value
     */
    public void setDescription(String value){
        if(description != null) {
            description = description + ", " + value;
        }else{
            description = value;
        }
    }
}
