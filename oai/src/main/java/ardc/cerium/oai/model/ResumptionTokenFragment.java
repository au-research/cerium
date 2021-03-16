package ardc.cerium.oai.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class ResumptionTokenFragment {

	@JacksonXmlProperty(isAttribute = true)
	private String completeListSize;

	@JacksonXmlProperty(isAttribute = true)
	private String cursor;

	@JacksonXmlText
	private String resumptionToken;

	public String getCompleteListSize() {
		return completeListSize;
	}

	public void setCompleteListSize(String completeListSize) {
		this.completeListSize = completeListSize;
	}

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}

	public String getResumptionToken() {
		return resumptionToken;
	}

	public void setResumptionToken(String resumptionToken) {
		this.resumptionToken = resumptionToken;
	}

	public void setToken(String completeListSize, String cursor, String value) {
		this.setCompleteListSize(completeListSize);
		this.setCursor(cursor);
		this.setResumptionToken(value);
	}

}
