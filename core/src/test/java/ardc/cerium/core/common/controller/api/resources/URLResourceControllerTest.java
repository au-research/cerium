package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.URLDTO;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.URLService;
import ardc.cerium.core.exception.ForbiddenOperationException;
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

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = URLResourceController.class)
@Import({ URLResourceController.class })
@AutoConfigureMockMvc
public class URLResourceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	URLService service;

	@MockBean
	RecordService recordService;

	@MockBean
	KeycloakService kcService;

	@Test
	public void it_should_return_a_url_when_get_by_id() throws Exception {
		URL url = TestHelper.mockUrl(UUID.randomUUID());
		when(service.findById(url.getId().toString())).thenReturn(url);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get("/api/resources/urls/" + url.getId().toString()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(url.getId().toString()));
	}

	@Test
	public void it_should_404_when_get_by_non_existence_uuid() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/api/resources/urls/" + UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void it_should_404_when_delete_by_non_existence_uuid() throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.delete("/api/resources/urls/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isNotFound());
	}

	@Test
	public void it_should_delete_a_url_when_delete_by_by_uuid() throws Exception {
		URL url = TestHelper.mockUrl();

		when(service.exists(url.getId().toString())).thenReturn(true);
		when(service.findById(url.getId().toString())).thenReturn(url);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.delete("/api/resources/urls/" + url.getId().toString()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isAccepted());
	}

	@Test
	public void store_RecordNotOwned_403() throws Exception {
		// given a user & record, no relation
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());

		// and a request dto of a new url for that record
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());

		// when service throws Forbidden, it bubbles up
		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);
		when(service.create(any(URLDTO.class), any(User.class))).thenThrow(ForbiddenOperationException.class);

		// when attempt to create a version
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/urls/")
				.param("recordID", record.getId().toString()).content(TestHelper.asJsonString(dto))
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isForbidden());
	}

	@Test
	public void store_validRequest_returnsDTO() throws Exception {

		URLDTO resultDTO = new URLDTO();
		resultDTO.setId(UUID.randomUUID());

		when(kcService.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(TestHelper.mockUser());
		when(service.create(any(URLDTO.class), any(User.class))).thenReturn(resultDTO);

		// mock a valid return from the service
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/resources/urls/")
				.content(TestHelper.asJsonString(new URLDTO())).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		mockMvc.perform(request).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
	}

	// todo update

}
