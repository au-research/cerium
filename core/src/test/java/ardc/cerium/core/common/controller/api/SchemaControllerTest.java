package ardc.cerium.core.common.controller.api;

import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SchemaController.class)
@Import({ SchemaService.class })
@AutoConfigureMockMvc
public class SchemaControllerTest {

	private final String baseUrl = "/api/resources/schemas/";

	@Autowired
	SchemaService schemaService;

	@MockBean
	KeycloakService kcService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void index_getAllSupportedSchemas() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(baseUrl)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request)
				// .andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$[*].id").exists());
	}

	@Test
	public void show_findASingleSchema_200() throws Exception {
		String schemaID = SchemaService.ARDCv1;
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(baseUrl + schemaID)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(schemaID));
	}

	@Test
	public void show_NonExistSchema_404() throws Exception {
		String schemaID = "non-exist";
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(baseUrl + schemaID)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void validate_validCSIROv3_200() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_igsn_csiro_v3.xml");

		String schemaID = SchemaService.CSIROv3;
		MockHttpServletRequestBuilder validRequest = MockMvcRequestBuilders.post(baseUrl + schemaID + "/validate")
				.content(validXML).contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(validRequest).andExpect(status().isOk());
	}

	@Test
	public void validate_invalidCSIROv3_400() throws Exception {
		String invalidXML = Helpers.readFile("src/test/resources/xml/invalid_sample_igsn_csiro_v3.xml");
		String schemaID = SchemaService.CSIROv3;
		MockHttpServletRequestBuilder invalidRequest = MockMvcRequestBuilders.post(baseUrl + schemaID + "/validate")
				.content(invalidXML).contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_JSON);
		mockMvc.perform(invalidRequest).andExpect(status().isBadRequest());
	}

	@Test
	public void validate_validARDCv1_200() throws Exception {
		String validXML = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		MockHttpServletRequestBuilder validRequest = MockMvcRequestBuilders
				.post(baseUrl + SchemaService.ARDCv1 + "/validate").content(validXML)
				.contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(validRequest).andExpect(status().isOk());
	}

}