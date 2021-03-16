package ardc.cerium.oai.response;

import ardc.cerium.oai.model.RequestFragment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Date;

@JsonRootName(value = "OAI-PMH")
public class OAIResponse {

	@JacksonXmlProperty(isAttribute = true, localName = "xmlns")
	private final String namespace = "http://www.openarchives.org/OAI/2.0/";

	@JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
	private final String xsi = "http://www.w3.org/2001/XMLSchema-instance";

	@JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
	private final String schemaLocation = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date responseDate;

	private RequestFragment request;

	public OAIResponse() {
		this.responseDate = new Date();
		this.request = new RequestFragment();
	}

	public Date getResponseDate() {
		return responseDate;
	}

	public void setResponseDate(Date responseDate) {
		this.responseDate = responseDate;
	}

	public RequestFragment getRequest() {
		return request;
	}

	public void setRequest(RequestFragment request) {
		this.request = request;
	}

}
