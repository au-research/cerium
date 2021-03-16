package ardc.cerium.core.common.dto;

import ardc.cerium.core.common.entity.Record;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RecordDTO {

	private UUID id;

	private boolean visible = true;

	private String title;

	@NotNull
	private Date createdAt;

	@NotNull
	private Date modifiedAt;

	@NotNull
	private UUID creatorID;

	@NotNull
	private UUID modifierID;

	@NotNull
	private UUID allocationID;

	@NotNull
	private Record.OwnerType ownerType;

	@NotNull
	private UUID ownerID;

	private String type;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<VersionDTO> currentVersions;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<IdentifierDTO> identifiers;

	private UUID requestID;

	public RecordDTO() {
		this.currentVersions = new ArrayList<>();
		this.identifiers = new ArrayList<>();
	}

	public UUID getId() {
		return id;
	}

	public Date getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Date modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public UUID getCreatorID() {
		return creatorID;
	}

	public void setCreatorID(UUID creatorID) {
		this.creatorID = creatorID;
	}

	public UUID getModifierID() {
		return modifierID;
	}

	public void setModifierID(UUID modifierID) {
		this.modifierID = modifierID;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public void setAllocationID(UUID allocationID) {
		this.allocationID = allocationID;
	}

	public Record.OwnerType getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(Record.OwnerType ownerType) {
		this.ownerType = ownerType;
	}

	public UUID getOwnerID() {
		return ownerID;
	}

	public void setOwnerID(UUID ownerID) {
		this.ownerID = ownerID;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<VersionDTO> getCurrentVersions() {
		return currentVersions;
	}

	public void setCurrentVersions(List<VersionDTO> currentVersions) {
		this.currentVersions = currentVersions;
	}

	public List<IdentifierDTO> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(List<IdentifierDTO> identifiers) {
		this.identifiers = identifiers;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

}
