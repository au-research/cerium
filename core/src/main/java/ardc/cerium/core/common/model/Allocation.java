package ardc.cerium.core.common.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Allocation implements Serializable {

	private final UUID id;

	private String name;

	private List<Scope> scopes;

	private String type;

	private String status;

	private Map<String, List<String>> attributes;

	private List<DataCenter> dataCenters;

	public Allocation(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Scope> getScopes() {
		return scopes;
	}

	public void setScopes(List<Scope> scopes) {
		this.scopes = scopes;
	}

	public Map<String, List<String>> getAttributes() {
		return this.attributes;
	}

	public void setAttributes(Map<String, List<String>> attributes) {
		this.attributes = attributes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<DataCenter> getDataCenters() {
		return dataCenters;
	}

	public void setDataCenters(List<DataCenter> dataCenters) {
		this.dataCenters = dataCenters;
	}
}
