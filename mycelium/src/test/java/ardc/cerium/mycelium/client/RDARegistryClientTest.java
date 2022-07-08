package ardc.cerium.mycelium.client;

import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.event.DataSourceUpdatedEvent;
import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RDARegistryClientTest {

    @Test
	void test_creation() {
		RDARegistryClient client = new RDARegistryClient("http://localhost");
		assertThat(client.getUrl()).isEqualTo("http://localhost");
		assertThat(client.getClient()).isInstanceOf(OkHttpClient.class);
	}

	@Test
	void test_webhook() throws IOException {
		RDARegistryClient client = new RDARegistryClient("http://localhost");

		final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
		final Call remoteCall = mock(Call.class);
		final Response response = new Response.Builder().request(new Request.Builder().url("http://localhost").build())
				.protocol(Protocol.HTTP_1_1).code(200).message("")
				.body(ResponseBody.create(MediaType.parse("application/json"), "OK"))
				.build();
		when(remoteCall.execute()).thenReturn(response);
		when(mockedHttpClient.newCall(any())).thenReturn(remoteCall);

		// client.getClient() now returns the mockedClient
		ReflectionTestUtils.setField(client, "client", mockedHttpClient);

		DataSourceUpdatedEvent event = new DataSourceUpdatedEvent(this, "1", "logMessage");

		// no exception when run
		client.sendWebHookRequest(event.toRDAEventDTO());
	}
}