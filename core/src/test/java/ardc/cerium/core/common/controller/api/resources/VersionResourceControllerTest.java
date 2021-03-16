package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.*;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = VersionResourceController.class)
@Import({ VersionMapper.class, SchemaService.class })
@AutoConfigureMockMvc
public class VersionResourceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	VersionService service;

	@MockBean
	KeycloakService kcService;

	@MockBean
	RecordService recordService;

	@Autowired
	VersionMapper versionMapper;

	// todo index
	// todo index_pagination
	// todo show_404
	// todo show
	// todo delete_403
	// todo delete_404
	// todo delete

	@Test
	public void store_UnknownRecord_404() throws Exception {
		// given a dto request
		VersionDTO versionDTO = new VersionDTO();
		versionDTO.setContent(Base64.getEncoder().encode("stuff".getBytes()).toString());
		versionDTO.setRecord(UUID.randomUUID().toString());
		versionDTO.setSchema("ardc.cerium.core.igsn-descriptive-v1");

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class))).thenThrow(new RecordNotFoundException("some-id"));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void store_forbidden_403() throws Exception {
		// given a dto request
		VersionDTO versionDTO = new VersionDTO();
		versionDTO.setContent(Base64.getEncoder().encode("stuff".getBytes()).toString());
		versionDTO.setRecord(UUID.randomUUID().toString());
		versionDTO.setSchema("ardc.cerium.core.igsn-descriptive-v1");

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class)))
				.thenThrow(new ForbiddenOperationException("some-id"));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionDTO)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isForbidden());
	}

	@Test
	public void store_ValidRequest_returns201WithLocation() throws Exception {
		// and a dto response
		Version mockedRecord = TestHelper.mockVersion(UUID.randomUUID());
		mockedRecord.setRecord(TestHelper.mockRecord(UUID.randomUUID()));

		// setup mocks
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(VersionDTO.class), any(User.class))).thenReturn(mockedRecord);

		// when POST
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/versions/")
				.content(TestHelper.asJsonString(versionMapper.convertToDTO(mockedRecord)))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		// expects 201 and Location header
		mockMvc.perform(request).andDo(MockMvcResultHandlers.print()).andExpect(status().isCreated())
				.andExpect(header().exists("Location"));
	}

}