package ardc.cerium.core.common.dto;

import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.Scope;

import java.util.List;
import java.util.UUID;

public class AllocationDTO {

	private UUID id;

	private String name;

	private List<Scope> scopes;

	private String type;

	private List<DataCenter> dataCenters;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<DataCenter> getDataCenters() {
		return dataCenters;
	}

	public void setDataCenters(List<DataCenter> dataCenters) {
		this.dataCenters = dataCenters;
	}
}
