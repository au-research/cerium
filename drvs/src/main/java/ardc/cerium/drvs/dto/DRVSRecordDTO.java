package ardc.cerium.drvs.dto;

import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.drvs.model.DRVSSubmission;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

public class DRVSRecordDTO {

	private UUID id;

	private boolean visible = true;

	private String title;

	@NotNull
	private Date createdAt;

	@NotNull
	private Date modifiedAt;

	private Date harvestedAt;

	@NotNull
	private UUID creatorID;

	@NotNull
	private UUID modifierID;

	private UUID requestID;

	private UUID allocationID;

	private DataCenter projectPartner;

	private DRVSSubmission submission;

	private String dataciteXML;

	private CollectionValidationSummaryDTO validation;

	// todo validation

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
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

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

	public DRVSSubmission getSubmission() {
		return submission;
	}

	public void setSubmission(DRVSSubmission submission) {
		this.submission = submission;
	}

	public String getDataciteXML() {
		return dataciteXML;
	}

	public void setDataciteXML(String dataciteXML) {
		this.dataciteXML = dataciteXML;
	}

	public DataCenter getProjectPartner() {
		return projectPartner;
	}

	public void setProjectPartner(DataCenter projectPartner) {
		this.projectPartner = projectPartner;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public void setAllocationID(UUID allocationID) {
		this.allocationID = allocationID;
	}

	public Date getHarvestedAt() {
		return harvestedAt;
	}

	public void setHarvestedAt(Date harvestedAt) {
		this.harvestedAt = harvestedAt;
	}

	public CollectionValidationSummaryDTO getValidation() {
		return validation;
	}

	public void setValidation(CollectionValidationSummaryDTO validation) {
		this.validation = validation;
	}

}
