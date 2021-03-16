package ardc.cerium.core.common.model;

import java.io.Serializable;
import java.util.UUID;

public class DataCenter implements Serializable {

	private final UUID id;

	private String name;

	private String path;

	public DataCenter(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
