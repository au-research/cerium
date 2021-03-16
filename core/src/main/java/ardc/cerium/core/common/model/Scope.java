package ardc.cerium.core.common.model;

import java.io.Serializable;

public enum Scope implements Serializable {

	UPDATE("update"), CREATE("create"), IMPORT("import");

	private final String value;

	Scope(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static Scope fromString(String text) {
		for (Scope b : Scope.values()) {
			if (b.value.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}

}
