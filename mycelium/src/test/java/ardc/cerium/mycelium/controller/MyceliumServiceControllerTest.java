package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.TreeNodeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.GraphServiceIT;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.VertexUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(MyceliumRegistryObjectResourceController.class)
@Import({TreeNodeDTOMapper.class, VertexDTOMapper.class, RegistryObjectVertexDTOMapper.class, ModelMapper.class})
public class MyceliumServiceControllerTest {


    public static String SERVICE_ENDPOINT = "/api/services/mycelium";

    @MockBean
    MyceliumService myceliumService;

    @MockBean
    GraphService graphService;

    @Autowired
    VertexDTOMapper vertexDTOMapper;

    @MockBean
    MyceliumIndexingService myceliumIndexingService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void getResolvedIdentifier() throws Exception {
        String completeUrl = SERVICE_ENDPOINT + "/resolve-identifiers?value=024nr0776&type=ror";

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(completeUrl)
                .accept(MediaType.APPLICATION_JSON);
        Vertex vertex = new Vertex();
        vertex.setIdentifier("024nr0776");
        vertex.setIdentifierType("ror");
        vertex.setStatus(Vertex.Status.PUBLISHED.name());
        graphService.ingestVertex(vertex);
        VertexUtil.resolveVertex(vertex);

        when(myceliumService.getIdentifierVertex("024nr0776","ror")).thenReturn(vertex);

        //mockMvc.perform(request)
            //    .andExpect(status().isOk());
               // .andExpect(jsonPath("$.identifier").value("1"));
    }
}
