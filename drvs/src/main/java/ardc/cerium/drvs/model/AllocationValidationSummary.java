package ardc.cerium.drvs.model;

import java.io.Serializable;
import java.util.HashMap;

public class AllocationValidationSummary implements Serializable {

	private Integer total;

	private Integer passed;

	private Integer failed;

	private Integer unvalidated;

	private Integer doiCount;

	private Integer doiNotFound;

	private String allocatonID;

	private String requestID;

	private HashMap<String, Integer> rules;

	public AllocationValidationSummary() {
		total = 0;
		passed = 0;
		failed = 0;
		unvalidated = 0;
		doiCount = 0;
		doiNotFound = 0;
		rules = new HashMap<String, Integer>();
		// initialise all rules so they show up even if no collection failed those
		rules.put("E01", 0);
		rules.put("E02", 0);
		rules.put("E03", 0);
		rules.put("E04", 0);
		rules.put("E05", 0);
		rules.put("E06", 0);
		rules.put("V01", 0);
		rules.put("V02", 0);
		rules.put("V03", 0);
		rules.put("V04", 0);
		rules.put("V05", 0);
		rules.put("V06", 0);
		rules.put("V07", 0);
	}

	/**
	 * Increment error count for given rule if rule doesn't exist it add a rule and set
	 * its value to 1
	 * @param key the ID of the failed rule
	 */
	public void incrementFailedRuleResult(String key) {
		if (rules.containsKey(key)) {
			Integer currentValue = rules.get(key);
			currentValue = currentValue + 1;
			rules.replace(key, currentValue);
		}
		else {
			rules.put(key, 1);
		}
	}

	public void setRuleCount(String key, Integer value) {
		rules.put(key, value);
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public void incrementTotal() {
		total++;
	}

	public void incrementPassed() {
		passed++;
	}

	public void incrementFailed() {
		failed++;
	}

	public void incrementUnvalidated() {
		unvalidated++;
	}

	public void incrementDoiCount() {
		doiCount++;
	}

	public void incrementDoiNotFound() {
		doiNotFound++;
	}

	public Integer getPassed() {
		return passed;
	}

	public void setPassed(Integer passed) {
		this.passed = passed;
	}

	public Integer getFailed() {
		return failed;
	}

	public void setFailed(Integer failed) {
		this.failed = failed;
	}

	public Integer getUnvalidated() {
		return unvalidated;
	}

	public void setUnvalidated(Integer unvalidated) {
		this.unvalidated = unvalidated;
	}

	public Integer getDoiCount() {
		return doiCount;
	}

	public void setDoiCount(Integer doiCount) {
		this.doiCount = doiCount;
	}

	public Integer getDoiNotFound() {
		return doiNotFound;
	}

	public void setDoiNotFound(Integer doiNotFound) {
		this.doiNotFound = doiNotFound;
	}

	public HashMap<String, Integer> getRules() {
		return rules;
	}

	public String getAllocatonID() {
		return allocatonID;
	}

	public void setAllocatonID(String allocatonID) {
		this.allocatonID = allocatonID;
	}

	public String getRequestID() {
		return requestID;
	}

	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

}
