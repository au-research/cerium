package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(RelationshipsAPIController.class)
class RelationshipsAPIControllerTest {

	public static String END_POINT = "/api/search/relationships";

	@MockBean
	MyceliumService myceliumService;

	@Autowired
	MockMvc mockMvc;

	@Captor
	private ArgumentCaptor<ArrayList<SearchCriteria>> searchCriteriaCaptor;

	@Test
	@DisplayName("Search should call the search method from myceliumService")
	void emptySearch() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(END_POINT)
				.accept(MediaType.APPLICATION_JSON);

		when(myceliumService.search(searchCriteriaCaptor.capture(), any(Pageable.class))).thenReturn(null);

		mockMvc.perform(request).andExpect(status().isOk());

		// no criteria should be passed in
		List<SearchCriteria> criteriaList = searchCriteriaCaptor.getValue();
		assertThat(criteriaList.size()).isEqualTo(0);

		verify(myceliumService, times(1)).search(any(List.class), any(Pageable.class));
	}

	@Test
	void fromIdentifierValueAndFromIdentifierTypeParameterShouldCreateTheRightSearchCriteria() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(END_POINT)
				.param("fromIdentifierValue", "fromValue").param("fromIdentifierType", "fromType")
				.accept(MediaType.APPLICATION_JSON);

		when(myceliumService.search(searchCriteriaCaptor.capture(), any(Pageable.class))).thenReturn(null);

		mockMvc.perform(request);

		// no criteria should be passed in
		List<SearchCriteria> criteriaList = searchCriteriaCaptor.getValue();
		assertThat(criteriaList.size()).isEqualTo(2);

		// fromIdentifierValue and fromIdentifierType search criteria should be available
		// in the list
		assertThat(criteriaList.stream().filter(searchCriteria -> searchCriteria.getKey().equals("fromIdentifierValue"))
				.findFirst().orElse(null)).isNotNull();
		assertThat(criteriaList.stream().filter(searchCriteria -> searchCriteria.getKey().equals("fromIdentifierType"))
				.findFirst().orElse(null)).isNotNull();
		assertThat(criteriaList.stream().filter(searchCriteria -> searchCriteria.getKey().equals("relationType"))
				.findFirst().orElse(null)).isNull();
	}

}