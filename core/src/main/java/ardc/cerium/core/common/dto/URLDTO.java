package ardc.cerium.core.common.dto;

import java.util.Date;
import java.util.UUID;

public class URLDTO {

	private UUID id;

	private String url;

	private boolean resolvable;

	private Date createdAt;

	private Date updatedAt;

	private Date checkedAt;

	private UUID record;

	public URLDTO() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isResolvable() {
		return resolvable;
	}

	public void setResolvable(boolean resolvable) {
		this.resolvable = resolvable;
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

	public Date getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Date checkedAt) {
		this.checkedAt = checkedAt;
	}

	public UUID getRecord() {
		return record;
	}

	public void setRecord(UUID record) {
		this.record = record;
	}

}
