package ardc.cerium.core.common.repository.specs;

public class SearchCriteria {

	private final String key;

	private final Object value;

	private final SearchOperation operation;

	public SearchCriteria(String key, Object value, SearchOperation operation) {
		this.key = key;
		this.value = value;
		this.operation = operation;
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public SearchOperation getOperation() {
		return operation;
	}

}
