package ardc.cerium.drvs.dto;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.drvs.model.CollectionValidationSummary;
import org.springframework.data.elasticsearch.annotations.*;

import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@Document(indexName = "drvs-records")
public class DRVSRecordDocument {

	@Id
	private String id;

	@MultiField(mainField = @Field(type = FieldType.Text),
			otherFields = { @InnerField(suffix = "raw", type = FieldType.Keyword) })
	private String title;

	@Field(type = FieldType.Keyword)
	private String status;

	@Field(type = FieldType.Date)
	private Date createdAt;

	@Field(type = FieldType.Date)
	private Date modifiedAt;

	@Field(type = FieldType.Keyword)
	private UUID creatorID;

	@Field(type = FieldType.Keyword)
	private UUID modifierID;

	@Field(type = FieldType.Keyword)
	private UUID allocationID;

	@Field(type = FieldType.Keyword)
	private Record.OwnerType ownerType;

	@Field(type = FieldType.Keyword)
	private UUID ownerID;

	@MultiField(mainField = @Field(type = FieldType.Text),
			otherFields = { @InnerField(suffix = "raw", type = FieldType.Keyword) })
	private String localCollectionID;

	@MultiField(mainField = @Field(type = FieldType.Text),
			otherFields = { @InnerField(suffix = "raw", type = FieldType.Keyword) })
	private String DOI;

	private CollectionValidationSummary validation;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getLocalCollectionID() {
		return localCollectionID;
	}

	public void setLocalCollectionID(String localCollectionID) {
		this.localCollectionID = localCollectionID;
	}

	public String getDOI() {
		return DOI;
	}

	public void setDOI(String DOI) {
		this.DOI = DOI;
	}

	public CollectionValidationSummary getValidation() {
		return validation;
	}

	public void setValidation(CollectionValidationSummary validation) {
		this.validation = validation;
	}
}
