package ardc.cerium.doi;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.doi.schema.citeproc.json.CiteProcJson;
import ardc.cerium.doi.schema.rdf.RDF;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentNegotiationClientTest {

	@Test
	void itReturnsRDF() throws IOException {

		// mock the call to DOIS RDF
		final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
		final Call remoteCall = mock(Call.class);
		final Response response = new Response.Builder().request(new Request.Builder().url("http://dois.org/").build())
				.protocol(Protocol.HTTP_1_1).code(200).message("")
				.body(ResponseBody.create(MediaType.parse("application/rdf+xml"),
						Helpers.readFile("src/test/resources/doi/10.1126.science.169.3946.635.xml")))
				.build();

		when(remoteCall.execute()).thenReturn(response);
		when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

		// when resolve
		ContentNegotiationClient client = new ContentNegotiationClient(mockedHttpClient);
		RDF rdf = client.resolveRDF("10.1126/science.169.3946.635");

		// rdf contains the required title
		assertThat(rdf).isNotNull();
		assertThat(rdf.getDescription().getTitle()).isEqualTo("The Structure of Ordinary Water");
	}

	@Test
	void itReturnsCiteprocJson() throws IOException {
        // mock the call to DOIS CiteProc JSON
        final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
        final Call remoteCall = mock(Call.class);
        final Response response = new Response.Builder().request(new Request.Builder().url("http://dois.org/").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("")
                .body(ResponseBody.create(MediaType.parse("application/vnd.citationstyles.csl+json"),
                        Helpers.readFile("src/test/resources/doi/10.1126.science.169.3946.635.json")))
                .build();

        when(remoteCall.execute()).thenReturn(response);
        when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

        // when resolve
		ContentNegotiationClient client = new ContentNegotiationClient(mockedHttpClient);
		CiteProcJson json = client.resolveCiteProcJson("10.1126/science.169.3946.635");

        // json contains the title
		assertThat(json).isNotNull();
		assertThat(json.getTitle()).isEqualTo("The Structure of Ordinary Water");
	}

}