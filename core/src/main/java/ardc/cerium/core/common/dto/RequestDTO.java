package ardc.cerium.core.common.dto;

import ardc.cerium.core.common.controller.api.resources.RequestResourceController;
import org.springframework.hateoas.RepresentationModel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class RequestDTO extends RepresentationModel<RequestDTO> {

	private UUID id;

	private String status;

	private String type;

	private UUID createdBy;

	private UUID allocationID;

	private Date createdAt;

	private Date updatedAt;

	private String message;

	private Map<String, String> summary = new HashMap<>();

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
		String uuid = getId().toString();
		add(linkTo(RequestResourceController.class).slash(uuid).withSelfRel());
		add(linkTo(RequestResourceController.class).slash(uuid).slash("logs").withRel("logs"));
		add(linkTo(RequestResourceController.class).slash(uuid).slash("identifiers").withRel("identifiers"));
		add(linkTo(RequestResourceController.class).slash(uuid).slash("records").withRel("records"));
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UUID createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, String> getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		HashMap<String, String> map = new HashMap<String, String>();
		if(summary == null){
			return;
		}
		String[] list = summary.split(",");
		if(list.length > 0){
			for (String item:list){
				String[] kv = item.split(":");
				if(kv.length > 1){
					map.put(kv[0].trim(), kv[1].trim());
				}
			}
		}
		this.summary = map;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public void setAllocationID(UUID allocationID) {
		this.allocationID = allocationID;
	}
}
