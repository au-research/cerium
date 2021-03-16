package ardc.cerium.core.common.model.schema;

import ardc.cerium.core.common.model.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class JSONSchema extends Schema {

	private String namespace;

	private String schemaLocation;

	private String localSchemaLocation;

	public JSONSchema() {

	}

	@JsonProperty("json")
	private void unpackXMLProperties(Map<String, String> json) {
		this.namespace = json.get("namespace");
		this.schemaLocation = json.get("schemaLocation");
		this.localSchemaLocation = json.get("localSchemaLocation");
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSchemaLocation() {
		return schemaLocation;
	}

	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	public String getLocalSchemaLocation() {
		return localSchemaLocation;
	}

	public void setLocalSchemaLocation(String localSchemaLocation) {
		this.localSchemaLocation = localSchemaLocation;
	}

}
