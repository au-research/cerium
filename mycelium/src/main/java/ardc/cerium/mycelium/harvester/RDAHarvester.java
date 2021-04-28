package ardc.cerium.mycelium.harvester;

import ardc.cerium.mycelium.model.RegistryObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class RDAHarvester {

	private final String url;

	public RDAHarvester(String url) {
		this.url = url;
	}

	public RegistryObject[] harvestRecords(Integer limit, Integer offset) {
		// @formatter:off
        HttpUrl.Builder url = new HttpUrl.Builder()
                .scheme("https")
                .host(this.url)
                .addPathSegment("api").addPathSegment("registry").addPathSegment("records");
        url.addQueryParameter("status", "PUBLISHED");
        url.addQueryParameter("limit", limit.toString());
        url.addQueryParameter("offset", offset.toString());
        // @formatter:on

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url.build()).build();
		Response response;
		try {
			response = client.newCall(request).execute();
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(response.body().string(), RegistryObject[].class);
		}
		catch (IOException e) {
			return null;
		}
	}

	public String harvestRIFCS(Long registryObjectId) {

		// @formatter:off
		HttpUrl.Builder url = new HttpUrl.Builder()
                .scheme("https")
                .host(this.url)
                .addPathSegment("api")
				.addPathSegment("registry")
                .addPathSegment("records")
                .addPathSegment(registryObjectId.toString())
				.addPathSegment("rifcs");
        // @formatter:on

		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url.build()).build();
		Response response;

		try {
			response = client.newCall(request).execute();
			return response.body().string();
		}
		catch (IOException e) {
			return null;
		}
	}

}
