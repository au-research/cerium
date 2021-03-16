package ardc.cerium.core.common.model;

import ardc.cerium.core.common.model.schema.JSONSchema;
import ardc.cerium.core.common.model.schema.XMLSchema;
import ardc.cerium.core.common.model.schema.TextSchema;
import ardc.cerium.core.common.provider.Metadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = XMLSchema.class, name = "XMLSchema"),
		@JsonSubTypes.Type(value = JSONSchema.class, name = "JSONSchema"),
		@JsonSubTypes.Type(value = TextSchema.class, name = "TextSchema")})
public class Schema {

	private String id;

	private String name;

	private String description;

	private String type;

	private Map<Metadata, String> providers;

	private Map<String, String> transforms;

	private String namespace;

	private String schemaLocation;

	private String oaiexport;

	public Schema() {
	}

	public Schema(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSchemaLocation() {
		return this.schemaLocation;
	}

	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<Metadata, String> getProviders() {
		return providers;
	}

	public void setProviders(Map<Metadata, String> providers) {
		this.providers = providers;
	}

	public Map<String, String> getTransforms() {
		return transforms;
	}

	public void setTransforms(Map<String, String> transforms) {
		this.transforms = transforms;
	}

	public String getOaiexport() {
		return oaiexport;
	}

	public void setOaiexport(String oaiexport) {
		this.oaiexport = oaiexport;
	}

}
