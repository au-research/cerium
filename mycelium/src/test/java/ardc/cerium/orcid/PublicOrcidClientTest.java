package ardc.cerium.orcid;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.orcid.schema.orcid.json.OrcidRecord;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicOrcidClientTest {

	@Test
	void testResolve() throws IOException {
        // mock the call to ORCID JSON
        final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
        final Call remoteCall = mock(Call.class);
        final Response response = new Response.Builder().request(new Request.Builder().url("http://dois.org/").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("")
                .body(ResponseBody.create(MediaType.parse("application/orcid+json"),
                        Helpers.readFile("src/test/resources/orcid/0000-0002-1825-0097.json")))
                .build();

        when(remoteCall.execute()).thenReturn(response);
        when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

        // when resolve
        PublicOrcidClient client = new PublicOrcidClient(mockedHttpClient);
        OrcidRecord record = client.resolve("0000-0002-1825-0097");

        // Orcidrecord contains names
        assertThat(record).isNotNull();
        assertThat(record.getPerson()).isNotNull();
        assertThat(record.getPerson().getName()).isNotNull();
        assertThat(record.getPerson().getName().getGivenName()).isEqualTo("Josiah");
        assertThat(record.getPerson().getName().getFamilyName()).isEqualTo("Carberry");
        assertThat(record.getPerson().getName().getFullName()).isEqualTo("Josiah Carberry");
	}
}