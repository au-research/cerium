package ardc.cerium.core.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "records",
		indexes = { @Index(name = "idx_owner", columnList = "ownerType,ownerId"),
				@Index(name = "idx_visible", columnList = "visible"),
				@Index(name = "idx_id_visible", columnList = "id,visible") })
public class Record {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
	private UUID id;

	private boolean visible = true;

	private String title;

	private String type;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date modifiedAt;

	@Column(columnDefinition = "BINARY(16)")
	private UUID creatorID;

	@Column(columnDefinition = "BINARY(16)")
	private UUID modifierID;

	@Column(columnDefinition = "BINARY(16)")
	private UUID allocationID;

	@Enumerated(EnumType.STRING)
	private OwnerType ownerType;

	@Column(columnDefinition = "BINARY(16)")
	private UUID ownerID;

	@JsonIgnore
	@OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
	private Set<Version> versions;

	@OneToMany(mappedBy = "record", fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Identifier> identifiers;

	@OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
	@Where(clause = "current = 1")
	private List<Version> currentVersions;

	@Column(columnDefinition = "BINARY(16)")
	private UUID requestID;

	/**
	 * Empty constructor
	 */
	public Record() {
		this.setType("Record");
	}

	/**
	 * Constructor with uuid Keep in mind the record once persist will have the uuid
	 * generated by Hibernate
	 * @param uuid the UUID to instantiate this record for
	 */
	public Record(UUID uuid) {
		this.id = uuid;
		this.setType("Record");
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Set<Version> getVersions() {
		return versions;
	}

	public void setVersions(Set<Version> versions) {
		this.versions = versions;
	}

	public UUID getCreatorID() {
		return creatorID;
	}

	/**
	 * Sets the CreatorID of the record
	 * @param createdBy UUID of the creator
	 */
	public void setCreatorID(UUID createdBy) {
		this.creatorID = createdBy;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Sets the creation date for the record
	 * @param createdAt the Date of creation
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Date updatedAt) {
		this.modifiedAt = updatedAt;
	}

	public UUID getAllocationID() {
		return allocationID;
	}

	public void setAllocationID(UUID allocationID) {
		this.allocationID = allocationID;
	}

	public UUID getModifierID() {
		return modifierID;
	}

	public void setModifierID(UUID modifiedBy) {
		this.modifierID = modifiedBy;
	}

	public OwnerType getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(OwnerType ownerType) {
		this.ownerType = ownerType;
	}

	public UUID getOwnerID() {
		return ownerID;
	}

	public void setOwnerID(UUID ownerID) {
		this.ownerID = ownerID;
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

	public List<Identifier> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(List<Identifier> identifiers) {
		this.identifiers = identifiers;
	}

	public List<Version> getCurrentVersions() {
		return currentVersions;
	}

	public void setCurrentVersions(List<Version> currentVersions) {
		this.currentVersions = currentVersions;
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

	public static enum OwnerType {

		User, DataCenter

	}

}
