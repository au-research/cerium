package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListRecordsFragment {

	@JsonProperty("record")
	@JacksonXmlElementWrapper(useWrapping = false)

	private List<RecordFragment> listRecords = new ArrayList<RecordFragment>();

	@JsonProperty("resumptionToken")
	@JacksonXmlElementWrapper(useWrapping = false)

	private ResumptionTokenFragment resumptionTokenFragment;

	public void setListRecords(RecordFragment recordFragment) {
		listRecords.add(recordFragment);
	}

	public void setResumptionTokenFragmentFragment(ResumptionTokenFragment resumptionTokenFragment) {
		this.resumptionTokenFragment = resumptionTokenFragment;
	}

	public ResumptionTokenFragment getResumptionTokenFragment() {
		return this.resumptionTokenFragment;
	}

}
