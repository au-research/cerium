package ardc.cerium.core.common.controller.api;

import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = { MeController.class })
@Import({ MeController.class })
@AutoConfigureMockMvc
public class MeControllerTest {

	private final String baseUrl = "/api/me/";

	@Autowired
	MockMvc mockMvc;

	@MockBean
	KeycloakService kcServiceMock;

	@Test
	public void it_should_return_me() throws Exception {

		// mock a user access
		UUID userUUID = UUID.randomUUID();
		User user = new User(userUUID);
		user.setRoles(Arrays.asList("IGSN_USER", "IGSN_ADMIN"));
		user.setName("Minh Duc Nguyen");
		user.setEmail("minh.nguyen@ardc.edu.au");

		// a test allocation
		Allocation testAllocation = new Allocation(UUID.randomUUID());
		testAllocation.setAttributes(new HashMap<String, List<String>>() {
			{
				put("key1", Collections.singletonList("value1"));
				put("key2", Arrays.asList("value1", "value2"));
			}
		});
		testAllocation.setScopes(Arrays.asList(Scope.UPDATE, Scope.CREATE));
		user.setAllocations(Collections.singletonList(new Allocation(UUID.randomUUID())));

		// data centers
		DataCenter testDataCenter = new DataCenter(UUID.randomUUID());
		testDataCenter.setName("TestGroup");
		user.setDataCenters(Collections.singletonList(testDataCenter));

		// mock the response
		when(kcServiceMock.getLoggedInUser(any(HttpServletRequest.class))).thenReturn(user);

		mockMvc.perform(MockMvcRequestBuilders.get(baseUrl)).andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print()).andExpect(jsonPath("$.id").value(userUUID.toString()))
				.andExpect(jsonPath("$.roles").exists()).andExpect(jsonPath("$.allocations.[*].id").exists())
				.andExpect(jsonPath("$.allocations.[*].name").exists())
				.andExpect(jsonPath("$.allocations.[*].scopes").exists())
				.andExpect(jsonPath("$.allocations.[*].attributes").doesNotExist())
				.andExpect(jsonPath("$.dataCenters.[*].id").exists())
				.andExpect(jsonPath("$.dataCenters.[*].name").exists());
	}

}