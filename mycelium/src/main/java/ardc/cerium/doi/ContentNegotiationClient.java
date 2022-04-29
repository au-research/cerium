package ardc.cerium.doi;

import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.doi.schema.rdf.RDF;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ContentNegotiationClient {

	private OkHttpClient client;

	public ContentNegotiationClient() {
		this.client = new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.readTimeout(15, TimeUnit.SECONDS)
				.build();
	}

	public ContentNegotiationClient(OkHttpClient client) {
		this.client = client;
	}

	public CiteProcJson resolveCiteProcJson(String doi) {
		try {
			String json = resolveString(doi, MetadataContentType.CITEPROC_JSON);
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(json, CiteProcJson.class);
		}
		catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	public RDF resolveRDF(String doi) {
		try {
			String xml = resolveString(doi, MetadataContentType.RDF_XML);
			XmlMapper xmlMapper = new XmlMapper();
			return xmlMapper.readValue(xml, RDF.class);
		}
		catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	public String resolveString(String doi, MetadataContentType contentType) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		String url = String.format("https://doi.org/%s", doi);
		Request request = new Request.Builder().url(url).addHeader("Accept", contentType.getContentType()).build();
		Response response = client.newCall(request).execute();
		String data = response.body().string();
		return data;
	}

}
