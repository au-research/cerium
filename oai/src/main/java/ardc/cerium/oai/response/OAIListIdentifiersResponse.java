package ardc.cerium.oai.response;

import ardc.cerium.oai.model.ListIdentifiersFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListIdentifiersResponse extends OAIResponse {

	@JsonProperty("ListIdentifiers")
	private ListIdentifiersFragment listIdentifiersFragment;

	public void setIdentifiersFragment(ListIdentifiersFragment listIdentifiersFragment) {
		this.listIdentifiersFragment = listIdentifiersFragment;
	}

	public ListIdentifiersFragment getListIdentifiersFragment() {
		return this.listIdentifiersFragment;
	}

}
