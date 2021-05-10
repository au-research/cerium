package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(RelationshipsAPIController.class)
class RelationshipsAPIControllerTest {

	public static String END_POINT = "/api/search/relationships";

	@MockBean
	MyceliumService myceliumService;

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("Search should call the search method from myceliumService")
	void name() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(END_POINT)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isOk());

		verify(myceliumService, times(1)).search(any(List.class), any(Pageable.class));
	}

}