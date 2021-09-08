package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumServiceController.class)
class MyceliumServiceControllerTest {

	public static String END_POINT = "/api/services/mycelium/";

	public static String IMPORT_ENDPOINT = END_POINT + "import-record";

	@MockBean
	MyceliumService myceliumService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@MockBean
	MyceliumIndexingService myceliumIndexingService;

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

		when(myceliumRequestService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);
		when(myceliumRequestService.save(any(Request.class))).thenReturn(mockedRequest);
		when(myceliumService.getRecordState(any(String.class))).thenReturn(mockedState);
		when(myceliumService.parsePayloadToRegistryObject(any(String.class))).thenReturn(registryObject);

		mockMvc.perform(
				MockMvcRequestBuilders.post(IMPORT_ENDPOINT).param("sideEffectRequestID", UUID.randomUUID().toString())
						.content(rifcs).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// todo fix importTask and verify RequestStatus
		// .andExpect(jsonPath("$.status").value(Request.Status.COMPLETED.toString()));

		// the request must also be validated
		verify(myceliumRequestService, times(1)).validateImportRequest(any(Request.class));
	}

    @Test
    @DisplayName("When an exception is thrown in the service, the handler returns a BadRequest well formed response")
    void importRequestValidated() throws Exception {
        String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
        Request mockedRequest = new Request();
        mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);
        when(myceliumRequestService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(IMPORT_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

}