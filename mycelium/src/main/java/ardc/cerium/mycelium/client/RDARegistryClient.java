package ardc.cerium.mycelium.client;

import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class RDARegistryClient {

	private final String url;

	public RDARegistryClient(String url) {
		this.url = url;
	}

	public RegistryObject[] getRegistryObjectByKey(String key) {
		log.debug("Resolving RegistryObject by Key: {}", key);
		// @formatter:off
        HttpUrl.Builder url = new HttpUrl.Builder()
				.scheme("http")
                .host(this.url)
                .addPathSegment("api").addPathSegment("registry").addPathSegment("records");
        url.addQueryParameter("key", key);
        // @formatter:on

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url.build()).build();
		Response response;
		try {
			response = client.newCall(request).execute();
			String data = response.body().string();
			log.debug("Data obtained {}", data);
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(data, RegistryObject[].class);
		}
		catch (IOException e) {
			log.error("Error Resolving RegistryObject by Key: {} Reason: {}", key, e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public RegistryObject getPublishedByKey(String key) {
		RegistryObject[] registryObjects = getRegistryObjectByKey(key);
		if (registryObjects == null) {
			return null;
		}

		return Arrays.stream(registryObjects).filter(registryObject -> registryObject.getStatus().equals("PUBLISHED"))
				.findFirst().orElse(null);
	}

	/**
	 * Sending a webhook event request
	 * @param event the {@link RDAEventDTO} that needs to be published
	 */
	public void sendWebHookRequest(RDAEventDTO event) {
		String endpoint = this.url + "/api/registry/webhook";
		log.debug("Sending webhook event[type={}] to endpoint[url={}]", event.getType(), endpoint);

		try {
			String eventJson = (new ObjectMapper()).writeValueAsString(event);
			Request request = new Request.Builder().url(endpoint).post(RequestBody.create(MediaType.parse("application/json"), eventJson)).build();
			OkHttpClient client = new OkHttpClient();
			Response response = client.newCall(request).execute();
			log.info("Received Webhook Response [code={}, body={}]", response.code(), response.body().string());
		} catch (JsonProcessingException e) {
			log.error("Failed to process event payload");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Failed to perform HTTP request");
			e.printStackTrace();
		}
	}

	@Async
	public void sendWebHookAsync(RDAEventDTO event) {
		sendWebHookRequest(event);
	}

}
