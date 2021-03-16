package ardc.cerium.oai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class RecordHeaderFragment {

	public String identifier;

	@JsonProperty("datestamp")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	public Date dateStamp;

	public RecordHeaderFragment(String identifier, Date dateStamp) {
		this.identifier = identifier;
		this.dateStamp = dateStamp;
	}

}
