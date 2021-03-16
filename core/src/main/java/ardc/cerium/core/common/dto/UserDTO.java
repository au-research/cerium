package ardc.cerium.core.common.dto;

import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;

import java.util.List;
import java.util.UUID;

public class UserDTO {

	private UUID id;

	private String username;

	private String email;

	private String name;

	private List<String> roles;

	private List<DataCenter> dataCenters;

	private List<AllocationDTO> allocations;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public List<DataCenter> getDataCenters() {
		return dataCenters;
	}

	public void setDataCenters(List<DataCenter> dataCenters) {
		this.dataCenters = dataCenters;
	}

	public List<AllocationDTO> getAllocations() {
		return allocations;
	}

	public void setAllocations(List<AllocationDTO> allocations) {
		this.allocations = allocations;
	}

}
