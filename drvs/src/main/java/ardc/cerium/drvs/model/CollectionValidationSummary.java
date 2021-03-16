package ardc.cerium.drvs.model;

import java.io.Serializable;
import java.util.HashMap;

public class CollectionValidationSummary implements Serializable {

	private Status status;

	private HashMap<String, Integer> counts;

	private HashMap<String, Boolean> rules;

	public CollectionValidationSummary() {
		status = Status.PASSED;
		counts = new HashMap<>();
		rules = new HashMap<>();
		rules.put("E01", false);
		rules.put("E02", false);
		rules.put("E03", false);
		rules.put("E04", false);
		rules.put("E05", false);
		rules.put("E06", false);
		rules.put("V01", false);
		rules.put("V02", false);
		rules.put("V03", false);
		rules.put("V04", false);
		rules.put("V05", false);
		rules.put("V06", false);
		rules.put("V07", false);
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public HashMap<String, Integer> getCounts() {
		counts.put("tests", rules.size());
		counts.put("passed", (int) rules.entrySet().stream().filter(entry -> entry.getValue().equals(true)).count());
		counts.put("failed", (int) rules.entrySet().stream().filter(entry -> entry.getValue().equals(false)).count());
		return counts;
	}

	public void setCounts(HashMap<String, Integer> counts) {
		this.counts = counts;
	}

	public HashMap<String, Boolean> setResult(String key, Boolean value) {
		rules.replace(key, value);
		if (!value) {
			status = Status.FAILED;
		}
		return rules;
	}

	public HashMap<String, Boolean> getRules() {
		return rules;
	}

	public void setRules(HashMap<String, Boolean> rules) {
		this.rules = rules;
	}

	public enum Status {
		FAILED, PASSED, UNVALIDATED, DOINOTFOUND
	}

}
