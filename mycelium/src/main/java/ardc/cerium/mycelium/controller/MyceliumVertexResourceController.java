package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.service.MyceliumService;
import com.google.common.base.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/resources/mycelium-vertex", produces = { MediaType.APPLICATION_JSON_VALUE })

public class MyceliumVertexResourceController {
    final MyceliumService myceliumService;
    final VertexDTOMapper vertexMapper;


    public MyceliumVertexResourceController(MyceliumService myceliumService,
                                            RegistryObjectVertexDTOMapper roMapper, VertexDTOMapper vertexMapper) {
        this.myceliumService = myceliumService;
        this.vertexMapper = vertexMapper;
    }

    @GetMapping(path = "/{id}/{type}")
    public ResponseEntity<?> getIdentifierVertex(
            @PathVariable("id") String identifier,
            @PathVariable("type") String type) {
        Vertex result = myceliumService.getGraphService().getVertexByIdentifier(identifier,type);
        Converter converter = vertexMapper.getConverter();
        return ResponseEntity.ok().body(converter.convert(result));
    }
}
