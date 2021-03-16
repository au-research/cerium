package ardc.cerium.oai.response;

import ardc.cerium.oai.model.ListMetadataFormatsFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIListMetadataFormatsResponse<metadataFormat> extends OAIResponse {

	@JsonProperty("ListMetadataFormats")
	private ListMetadataFormatsFragment listMetadataFormatsFragment;

	public void setListMetadataFormatsFragment(ListMetadataFormatsFragment listMetadataFormatsFragment) {
		this.listMetadataFormatsFragment = listMetadataFormatsFragment;
	}

	public ListMetadataFormatsFragment getListMetadataFormatsFragment() {
		return this.listMetadataFormatsFragment;
	}

}
