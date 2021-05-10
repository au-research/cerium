package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.controller.api.resources.RecordResourceController;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.service.MyceliumService;
import org.aspectj.util.FileUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(ImportAPIController.class)
class ImportAPIControllerTest {

    @MockBean
    MyceliumService myceliumService;

    @Autowired
    MockMvc mockMvc;

    public static String END_POINT = "/api/services/import";

    @Test
    @DisplayName("Given a payload, when importing returns a COMPLETED request")
	void importRequestCompleted() throws Exception {

        String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
        String rifcs = Helpers.readFile(scenario1Path);

        Request mockedRequest = new Request();
        mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);
        when(myceliumService.createImportRequest(any(String.class))).thenReturn(mockedRequest);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(END_POINT).content(rifcs)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(Request.Status.COMPLETED.toString()));
	}
}