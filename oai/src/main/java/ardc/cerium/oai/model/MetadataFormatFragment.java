package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "metadataPrefix", "schema", "metadataNamespace" })
public class MetadataFormatFragment {

	@JsonProperty("metadataPrefix")
	private String metadataPrefix;

	@JsonProperty("schema")
	private String schema;

	@JsonProperty("metadataNamespace")
	private String metadataNamespace;

	public void setFormat(String metadataPrefix, String schema, String metadataNamespace) {
		this.metadataPrefix = metadataPrefix;
		this.schema = schema;
		this.metadataNamespace = metadataNamespace;
	}

	public String getMetadataPrefix() {
		return this.metadataPrefix;
	}

	public String getSchema() {
		return this.schema;
	}

	public String getMetadataNamespace() {
		return this.metadataNamespace;
	}

}
