package ardc.cerium.core.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User {

	private UUID id;

	private String username;

	private String email;

	private String name;

	private List<String> roles;

	private List<DataCenter> dataCenters;

	private List<Allocation> allocations;

	public User() {

	}

	public User(UUID id) {
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

	public UUID getId() {
		return id;
	}

	public List<DataCenter> getDataCenters() {
		return dataCenters;
	}

	public void setDataCenters(List<DataCenter> dataCenters) {
		this.dataCenters = dataCenters;
	}

	public List<Allocation> getAllocations() {
		return allocations;
	}

	public void setAllocations(List<Allocation> allocations) {
		this.allocations = allocations;
	}

	/**
	 * Returns if the user has the DataCenter is in the User list
	 * @param dataCenterID the uuid of the data center
	 * @return true if the DataCenter is present
	 */
	public boolean belongsToDataCenter(UUID dataCenterID) {
		for (DataCenter dc : dataCenters) {
			if (dc.getId().equals(dataCenterID)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns if the User has access to the Allocation by the provided ID
	 * @param allocationID the uuid of the Allocation
	 * @return true if the Allocation is present
	 */
	public boolean hasAllocation(UUID allocationID) {
		for (Allocation allocation : allocations) {
			if (allocation.getId().equals(allocationID)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the Allocation that the User has access to by ID
	 * @param allocationID the UUID of the Allocation
	 * @return the Requested Allocation
	 */
	public Allocation getAllocationById(UUID allocationID) {
		for (Allocation allocation : allocations) {
			if (allocation.getId().equals(allocationID)) {
				return allocation;
			}
		}

		return null;
	}

	/**
	 * Returns the Allocations by type
	 * @param type the type of the Allocation
	 * @return the Requested Allocation as a List
	 */
	public List<Allocation> getAllocationsByType(String type) {
		List<Allocation> aList = new ArrayList<Allocation>();
		for (Allocation allocation : allocations) {
			if (allocation.getType().equals(type)) {
				aList.add(allocation);
			}
		}
		return aList;
	}

}
