package ardc.cerium.core.common.entity;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "identifiers",
		indexes = { @Index(name = "idx_status", columnList = "status"),
				@Index(name = "idx_type_value", columnList = "type,value"),
				@Index(name = "idx_type_status", columnList = "type,status") },
		uniqueConstraints = @UniqueConstraint(columnNames = { "value", "type", "domain" }))
public class Identifier {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false, unique = true)
	private UUID id;

	@Enumerated(EnumType.STRING)
	private Type type;

	@Enumerated(EnumType.STRING)
	private Status status;

	private String value;

	private String domain = "local";

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "record_id", nullable = false)
	private Record record;

	@Column(columnDefinition = "BINARY(16)")
	private UUID requestID;

	/**
	 * Empty constructor
	 */
	public Identifier() {

	}

	/**
	 * Constructor with uuid Keep in mind the identifier once persist will have the uuid
	 * generated by Hibernate
	 * @param uuid the UUID to instantiate this identifier for
	 */
	public Identifier(UUID uuid) {
		this.id = uuid;
	}

	public UUID getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date created) {
		this.createdAt = created;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updated) {
		this.updatedAt = updated;
	}

	public Record getRecord() {
		return record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public enum Type {

		IGSN, DRVS, PID, DOI, RAID

	}

	public enum Status {

		RESERVED, PENDING, ACCESSIBLE

	}

}
