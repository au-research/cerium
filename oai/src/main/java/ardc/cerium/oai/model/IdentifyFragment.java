package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;

@JsonPropertyOrder({ "repositoryName", "baseURL", "protocolVersion", "adminEmail", "earliestDatestamp", "deletedRecord",
		"granularity" })
public class IdentifyFragment {

	private String repositoryName;

	private String baseURL;

	private String protocolVersion;

	private String adminEmail;

	@JsonProperty("earliestDatestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	public Date earliestDatestamp;

	private String deletedRecord;

	private String granularity;

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	public Date getEarliestDatestamp() {
		return earliestDatestamp;
	}

	public void setEarliestDatestamp(Date earliestDatestamp) {
		this.earliestDatestamp = earliestDatestamp;
	}

	public String getDeletedRecord() {
		return deletedRecord;
	}

	public void setDeletedRecord(String deletedRecord) {
		this.deletedRecord = deletedRecord;
	}

	public String getGranularity() {
		return granularity;
	}

	public void setGranularity(String granularity) {
		this.granularity = granularity;
	}

}
