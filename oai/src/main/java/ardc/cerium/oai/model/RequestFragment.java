package ardc.cerium.oai.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class RequestFragment {

	@JacksonXmlProperty(isAttribute = true)
	private String verb;

	@JacksonXmlProperty(isAttribute = true)
	private String identifier;

	@JacksonXmlProperty(isAttribute = true)
	private String metadataPrefix;

	@JacksonXmlText
	private String value;

	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getMetadataPrefix() {
		return metadataPrefix;
	}

	public void setMetadataPrefix(String metadataPrefix) {
		this.metadataPrefix = metadataPrefix;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
