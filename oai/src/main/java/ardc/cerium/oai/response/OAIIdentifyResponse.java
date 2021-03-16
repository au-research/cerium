package ardc.cerium.oai.response;

import ardc.cerium.oai.model.IdentifyFragment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OAIIdentifyResponse extends OAIResponse {

	@JsonProperty("Identify")
	private IdentifyFragment identify;

	public OAIIdentifyResponse(IdentifyFragment identify) {
		this.identify = identify;
	}

	public IdentifyFragment getIdentify() {
		return identify;
	}

}
