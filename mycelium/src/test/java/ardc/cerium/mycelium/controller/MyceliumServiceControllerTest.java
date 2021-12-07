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