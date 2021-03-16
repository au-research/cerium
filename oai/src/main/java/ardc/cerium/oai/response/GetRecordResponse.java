package ardc.cerium.oai.response;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.oai.model.GetRecordFragment;
import ardc.cerium.oai.model.RecordFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetRecordResponse extends OAIResponse {

	@JsonProperty("GetRecord")
	private GetRecordFragment recordFragment;

	private Record record;

	private String metadata;

	public GetRecordResponse(Record record, String metadata) {
		this.recordFragment = new GetRecordFragment();
		RecordFragment recordFragment = new RecordFragment(record, metadata);
		this.recordFragment.setRecord(recordFragment);
	}

}
