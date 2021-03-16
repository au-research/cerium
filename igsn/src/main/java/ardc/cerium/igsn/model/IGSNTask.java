package ardc.cerium.igsn.model;

import java.io.File;
import java.util.UUID;

public class IGSNTask {

	public static final String TASK_IMPORT = "task.import";

	public static final String TASK_SYNC = "task.sync";

	public static final String TASK_UPDATE = "task.update";

	public static final String TASK_RESERVE = "task.reserve";

	public static final String TASK_TRANSFER = "task.transfer";

	private String type;

	private String identifierValue;

	private UUID requestID;

	private File contentFile;

	public IGSNTask() {
	}

	public IGSNTask(String type, String identifierValue, UUID requestID) {
		this.type = type;
		this.identifierValue = identifierValue;
		this.requestID = requestID;
	}

	public IGSNTask(String type, File contentPath, UUID requestID) {
		this.type = type;
		this.contentFile = contentPath;
		this.requestID = requestID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIdentifierValue() {
		return identifierValue;
	}

	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
	}

	public UUID getRequestID() {
		return requestID;
	}

	public void setRequestID(UUID requestID) {
		this.requestID = requestID;
	}

	@Override
	public String toString() {
		return "IGSNTask{" + "type='" + type + '\'' + ", identifierValue='" + identifierValue + '\'' + ", requestID="
				+ requestID;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IGSNTask) {
			IGSNTask other = (IGSNTask) obj;
			return type.equals(other.getType()) && identifierValue.equals(other.getIdentifierValue());
		}
		return false;
	}

	public File getContentFile() {
		return contentFile;
	}

	public void setContentFile(File contentFile) {
		this.contentFile = contentFile;
	}

}
