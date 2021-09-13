package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumServiceController.class)
class MyceliumServiceControllerTest {

	public static String END_POINT = "/api/services/mycelium/";

	public static String IMPORT_ENDPOINT = END_POINT + "import-record";

	@MockBean
	MyceliumService myceliumService;

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

		RecordState mockedState = new RecordState();
		mockedState.setRegistryObjectId("1");

		RegistryObject registryObject = new RegistryObject();
		registryObject.setRegistryObjectId(1L);

		when(myceliumService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);
		when(myceliumService.save(any(Request.class))).thenReturn(mockedRequest);
		when(myceliumService.getRecordState(any(String.class))).thenReturn(mockedState);
		when(myceliumService.parsePayloadToRegistryObject(any(String.class))).thenReturn(registryObject);
		doAnswer(invocationOnMock -> {
			mockedRequest.setStatus(Request.Status.COMPLETED);
			return null;
		}).when(myceliumService).runImportTask(mockedRequest);

		// @formatter:off
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.post(IMPORT_ENDPOINT)
				.param("sideEffectRequestID", UUID.randomUUID().toString())
				.content(rifcs)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(Request.Status.COMPLETED.toString()));
		// @formatter:on

		// the request must also be validated
		verify(myceliumService, times(1)).validateRequest(any(Request.class));
	}

	@Test
	@DisplayName("When an exception is thrown in the service, the handler returns a BadRequest well formed response")
	void importRequestValidated() throws Exception {
		String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
		Request mockedRequest = new Request();
		mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);
		when(myceliumService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(IMPORT_ENDPOINT)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isBadRequest());
	}

}