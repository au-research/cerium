package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumDataSourceResourceController.class)
class MyceliumDataSourceResourceControllerTest {

	public static String END_POINT = "/api/resources/mycelium-datasources";

	@MockBean
	MyceliumService myceliumService;

	@Autowired
	MockMvc mockMvc;

	@Test
	void itShowsAll() throws Exception {

		DataSource ds = new DataSource();
		ds.setId("1");

		when(myceliumService.getDataSources()).thenReturn(List.of(ds));

		// @formatter:off
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(END_POINT)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].id").value("1"));
        // @formatter:on
	}

	@Test
	void itShowsOne() throws Exception {

		when(myceliumService.getDataSourceById(anyString())).thenAnswer(invocation -> {
			DataSource ds = new DataSource();
			String id = invocation.getArgument(0);
			ds.setId(id);
			ds.setTitle(String.format("DataSource %s", id));
			return ds;
		});

		// @formatter:off
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(END_POINT + "/" + "1")
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("DataSource 1"));
        // @formatter:on
	}

    // todo create
    // todo update
    // todo delete

}