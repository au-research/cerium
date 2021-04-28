package ardc.cerium.mycelium.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RegistryObjects {

    @JacksonXmlProperty(localName = "registryObject")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<RegistryObject> registryObjects = new ArrayList<>();
}
