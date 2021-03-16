package ardc.cerium.drvs.dto;

import ardc.cerium.drvs.model.CollectionValidationSummary;

import java.util.HashMap;

public class CollectionValidationSummaryDTO {

	public CollectionValidationSummary.Status status;

	public HashMap<String, Integer> counts;

	public HashMap<String, Boolean> rules;

	public CollectionValidationSummaryDTO() {
		status = CollectionValidationSummary.Status.UNVALIDATED;
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

	public CollectionValidationSummary.Status getStatus() {
		return status;
	}

	public void setStatus(CollectionValidationSummary.Status status) {
		this.status = status;
	}

	public HashMap<String, Integer> getCounts() {
		return counts;
	}

	public void setCounts(HashMap<String, Integer> counts) {
		this.counts = counts;
	}

	public HashMap<String, Boolean> getRules() {
		return rules;
	}

	public void setRules(HashMap<String, Boolean> rules) {
		this.rules = rules;
	}
}
