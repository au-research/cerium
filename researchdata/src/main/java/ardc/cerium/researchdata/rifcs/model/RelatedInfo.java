package ardc.cerium.researchdata.rifcs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class RelatedInfo {

	@JacksonXmlProperty(localName = "identifier")
	@JacksonXmlElementWrapper(useWrapping = false)
	private List<Identifier> identifiers;

	@JacksonXmlProperty(localName = "relation")
	@JacksonXmlElementWrapper(useWrapping = false)
	private List<Relation> relation;

	private String title;

	private String notes;

}
