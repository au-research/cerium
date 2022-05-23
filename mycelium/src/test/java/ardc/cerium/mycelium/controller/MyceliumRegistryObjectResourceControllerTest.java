package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.TreeNodeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumRegistryObjectResourceController.class)
@Import({TreeNodeDTOMapper.class, VertexDTOMapper.class, RegistryObjectVertexDTOMapper.class, ModelMapper.class})
class MyceliumRegistryObjectResourceControllerTest {

	public static String RESOURCE_ENDPOINT = "/api/resources/mycelium-registry-objects";

	@MockBean
	MyceliumService myceliumService;

	@Autowired
	TreeNodeDTOMapper treeNodeDTOMapper;

	@MockBean
	RegistryObjectVertexDTOMapper registryObjectVertexDTOMapper;

	@Autowired
	VertexDTOMapper vertexDTOMapper;

	@Autowired
	MockMvc mockMvc;

	@Test
	@DisplayName("When an exception is thrown in the service, the handler returns a BadRequest well formed response")
	void importRequestValidated() throws Exception {
		String scenario1Path = "src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml";
		Request mockedRequest = new Request();
		mockedRequest.setAttribute(Attribute.PAYLOAD_PATH, scenario1Path);
		when(myceliumService.createRequest(any(RequestDTO.class))).thenReturn(mockedRequest);

		mockMvc.perform(post(RESOURCE_ENDPOINT).accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void getRegistryObjects() throws Exception {
		GraphService graphService = Mockito.mock(GraphService.class);
		when(graphService.getAllRegistryObjects(any(Pageable.class))).thenReturn(Mockito.mock(Page.class));
		when(myceliumService.getGraphService()).thenReturn(graphService);
		mockMvc.perform(get(RESOURCE_ENDPOINT).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

	@Test
	void getRegistryObjectById() throws Exception {
		Vertex vertex = new Vertex();
		vertex.setIdentifier("1");
		vertex.setIdentifierType("ro:id");
		when(myceliumService.getVertexFromRegistryObjectId(any(String.class))).thenReturn(vertex);
		mockMvc.perform(get(RESOURCE_ENDPOINT + "/" + "1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.identifier").value("1"));
	}

	@Test
	void deleteRegistryObjectById() throws Exception {
		MyceliumRequestService requestService = Mockito.mock(MyceliumRequestService.class);
		when(requestService.findById(any())).thenReturn(null);
		when(myceliumService.getMyceliumRequestService()).thenReturn(requestService);
		mockMvc.perform(delete(RESOURCE_ENDPOINT + "/" + "1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
		verify(myceliumService, times(1)).runDeleteTask("1", null);
	}

	@Test
	void getRegistryObjectIdentifiers() throws Exception {
		Collection<Vertex> identifiers = new ArrayList<>();
		identifiers.add(new Vertex("123", "doi"));
		identifiers.add(new Vertex("345", "orcid"));
		GraphService graphService = Mockito.mock(GraphService.class);
		when(graphService.getSameAs("1", "ro:id")).thenReturn(identifiers);
		when(myceliumService.getGraphService()).thenReturn(graphService);
		mockMvc.perform(get(RESOURCE_ENDPOINT + "/1/identifiers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void getRegistryObjectDuplicates() throws Exception {
		Collection<Vertex> duplicates = new ArrayList<>();
		duplicates.add(new Vertex("123", "ro:id"));
		duplicates.add(new Vertex("345", "ro:id"));
		duplicates.add(new Vertex("567", "ro:id"));
		GraphService graphService = Mockito.mock(GraphService.class);
		when(graphService.getDuplicateRegistryObject(any(Vertex.class))).thenReturn(duplicates);
		when(myceliumService.getVertexFromRegistryObjectId("1")).thenReturn(new Vertex());
		when(myceliumService.getGraphService()).thenReturn(graphService);
		mockMvc.perform(get(RESOURCE_ENDPOINT + "/1/duplicates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}
}