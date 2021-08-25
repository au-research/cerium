package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.util.NestedServletException;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(ImportAPIController.class)
class ImportAPIControllerTest {

	public static String END_POINT = "/api/services/mycelium/import-record";

	@MockBean
	MyceliumService myceliumService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("Given a payload, when importing returns 200")
	void importRequestCompleted() throws Exception {

		String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
		String rifcs = Helpers.readFile(scenario1Path);

		Request mockedRequest = new Request();
		mockedRequest.setStatus(Request.Status.COMPLETED);
		mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);

		when(myceliumRequestService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);
		when(myceliumRequestService.save(any(Request.class))).thenReturn(mockedRequest);

		mockMvc.perform(MockMvcRequestBuilders
						.post(END_POINT)
						.param("sideEffectRequestID", UUID.randomUUID().toString())
						.content(rifcs)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// todo fix importTask and verify RequestStatus
//				.andExpect(jsonPath("$.status").value(Request.Status.COMPLETED.toString()));

		// the request must also be validated
		verify(myceliumService, times(1)).validateRequest(any(Request.class));
	}

	@Test
	@DisplayName("When an exception is thrown in the service, the handler returns a BadRequest well formed response")
	void importRequestValidated() throws Exception {
		String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
		Request mockedRequest = new Request();
		mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);
		when(myceliumRequestService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);

		doThrow(new ContentNotSupportedException("something wrong")).when(myceliumService).validateRequest(any(Request.class));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(END_POINT)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}
}