package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListIdentifiersFragment {

	@JsonProperty("header")
	@JacksonXmlElementWrapper(useWrapping = false)

	private List<RecordHeaderFragment> listIdentifiers = new ArrayList<RecordHeaderFragment>();

	@JsonProperty("resumptionToken")
	@JacksonXmlElementWrapper(useWrapping = false)
	private ResumptionTokenFragment resumptionTokenFragment;

	public void setListIdentifiers(RecordHeaderFragment recordHeaderFragment) {
		listIdentifiers.add(recordHeaderFragment);
	}

	public void setResumptionTokenFragmentFragment(ResumptionTokenFragment resumptionTokenFragment) {
		this.resumptionTokenFragment = resumptionTokenFragment;
	}

	public ResumptionTokenFragment getResumptionTokenFragment() {
		return this.resumptionTokenFragment;
	}

}
