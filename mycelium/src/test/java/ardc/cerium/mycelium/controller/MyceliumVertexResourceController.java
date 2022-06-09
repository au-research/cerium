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

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumVertexResourceController.class)
@Import({TreeNodeDTOMapper.class, VertexDTOMapper.class, RegistryObjectVertexDTOMapper.class, ModelMapper.class})
public class MyceliumVertexResourceController {

    public static String RESOURCE_ENDPOINT = "/api/resources/mycelium-vertex";

    @MockBean
    MyceliumService myceliumService;

    @Autowired
    VertexDTOMapper vertexDTOMapper;

    @Autowired
    MockMvc mockMvc;


    void getIdentifierVertex() throws Exception {
        Collection<Vertex> identifiers = new ArrayList<>();
        identifiers.add(new Vertex("123", "doi"));
        identifiers.add(new Vertex("345", "orcid"));
        GraphService graphService = Mockito.mock(GraphService.class);
        when(graphService.getSameAs("123", "doi")).thenReturn(identifiers);
        when(myceliumService.getGraphService()).thenReturn(graphService);
        mockMvc.perform(get(RESOURCE_ENDPOINT + "/123/doi"))
                .andExpect(status().isOk());
               // .andExpect(jsonPath("$.length()").value(2));
    }
}
