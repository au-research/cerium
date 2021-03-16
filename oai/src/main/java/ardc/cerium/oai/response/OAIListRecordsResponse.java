package ardc.cerium.oai.response;

import ardc.cerium.oai.model.ListRecordsFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListRecordsResponse extends OAIResponse {

	@JsonProperty("ListRecords")
	private ListRecordsFragment listRecordsFragment;

	public void setRecordsFragment(ListRecordsFragment listRecordsFragment) {
		this.listRecordsFragment = listRecordsFragment;
	}

	public ListRecordsFragment getListRecordsFragment() {
		return this.listRecordsFragment;
	}

}
