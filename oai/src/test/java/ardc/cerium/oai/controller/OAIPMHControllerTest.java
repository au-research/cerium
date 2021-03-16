package ardc.cerium.oai.controller;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.oai.service.OAIPMHService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = OAIPMHController.class)
@AutoConfigureMockMvc
class OAIPMHControllerTest {

	final String base_url = "/api/services/oai-pmh";

	@Autowired
	MockMvc mockMvc;

	@MockBean
	SchemaService schemaService;

	@MockBean
	RecordService recordService;

	@MockBean
	VersionService versionService;

	@MockBean
	ApplicationProperties applicationProperties;

	@MockBean
	OAIPMHService oaipmhService;

	@Test
	@DisplayName("Throws an exception and returns the error element with badverb code attribute")
	void test_handle_noVerbParam_throwsException() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(base_url)
				.contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);

		mockMvc.perform(request).andDo(print()).andExpect(content().contentType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/OAI-PMH/error[@code='badVerb']").exists()).andExpect(status().isOk());
	}

	@Test
	void handle_noVerb_throwsException() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(base_url + "/?verb=nonsense")
				.contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);

		mockMvc.perform(request).andDo(print()).andExpect(content().contentType(MediaType.APPLICATION_XML))
				.andExpect(xpath("/OAI-PMH/error[@code='badVerb']").exists()).andExpect(status().isOk());
	}

}