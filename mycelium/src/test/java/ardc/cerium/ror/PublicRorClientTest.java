package ardc.cerium.ror;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.ror.PublicRorClient;
import ardc.cerium.ror.schema.ror.json.RorRecord;
import okhttp3.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicRorClientTest {

    @Test
    void testResolve() throws IOException {
        // mock the call to ROR JSON
        final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
        final Call remoteCall = mock(Call.class);
        final Response response = new Response.Builder().request(new Request.Builder().url("https://api.ror.org/organizations").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("")
                .body(ResponseBody.create(MediaType.parse("application/ror+json"),
                        Helpers.readFile("src/test/resources/ror/015w2mp89.json")))
                .build();

        when(remoteCall.execute()).thenReturn(response);
        when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

        // when resolve
        PublicRorClient client = new PublicRorClient(mockedHttpClient);
        RorRecord record = client.resolve("015w2mp89");
        assertThat(record).isNotNull();
        // RorRecord contains a name
        assertThat(record.getId()).isNotNull();
        // RorRecord contains an identifier
        assertThat(record.getName()).isNotNull();
    }

    @Test
    void testResolveNameNotNull() throws IOException {
        // mock the call to ORCID JSON
        final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
        final Call remoteCall = mock(Call.class);
        final Response response = new Response.Builder().request(new Request.Builder().url("http://api.ror.org/organisations").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("")
                .body(ResponseBody.create(MediaType.parse("application/orcid+json"),
                        Helpers.readFile("src/test/resources/ror/01pmm8272.json")))
                .build();

        when(remoteCall.execute()).thenReturn(response);
        when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

        // when resolve
        PublicRorClient client = new PublicRorClient(mockedHttpClient);
        RorRecord record = client.resolve("https://ror.org/01pmm8272");
        String expected_id = "https://ror.org/01pmm8272";
        String expected_name = "Seeburg Castle University";

        assertThat(record).isNotNull();

        // Ror record contains an id
        assertThat(record.getId()).isNotNull();
        assertThat(record.getId()).isEqualTo(expected_id);

        // Ror record contains a name
        assertThat(record.getName()).isNotNull();
        assertThat(record.getName()).isEqualTo(expected_name);

    }
}