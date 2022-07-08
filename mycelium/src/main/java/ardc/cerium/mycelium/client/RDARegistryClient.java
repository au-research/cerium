package ardc.cerium.mycelium.client;

import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Getter
public class RDARegistryClient {

	private final String url;

	private OkHttpClient client;

	public RDARegistryClient(String url) {
		this.url = url;
		client = new OkHttpClient();
	}

	/**
	 * Sending a webhook event request
	 * @param event the {@link RDAEventDTO} that needs to be published
	 */
	public void sendWebHookRequest(RDAEventDTO event) {
		String endpoint = this.getUrl() + "/api/registry/webhook";
		log.debug("Sending webhook event[type={}] to endpoint[url={}]", event.getType(), endpoint);

		try {
			String eventJson = (new ObjectMapper()).writeValueAsString(event);
			Request request = new Request.Builder().url(endpoint).post(RequestBody.create(MediaType.parse("application/json"), eventJson)).build();
			Response response = this.getClient().newCall(request).execute();
			log.info("Received Webhook Response [code={}, body={}]", response.code(), response.body().string());
		} catch (JsonProcessingException e) {
			log.error("Failed to process event payload");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Failed to perform HTTP request");
			e.printStackTrace();
		}
	}

}
