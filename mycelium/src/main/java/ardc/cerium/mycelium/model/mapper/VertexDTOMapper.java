package ardc.cerium.mycelium.model.mapper;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.VertexDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Converter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class VertexDTOMapper {

    final ModelMapper modelMapper;

    final ObjectMapper objectMapper;

    public Converter<Vertex, VertexDTO> converter;

    public VertexDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.objectMapper = new ObjectMapper();
        this.converter = buildConverter();
    }

    public Converter<Vertex, VertexDTO> getConverter() {
        return converter;
    }

    private Converter<Vertex, VertexDTO> buildConverter() {
        return new Converter<Vertex, VertexDTO>() {
            @Override
            protected VertexDTO doForward(Vertex vertex) {
                VertexDTO dto = modelMapper.map(vertex, VertexDTO.class);

                try {
                    Map<String, Object> map = objectMapper.readValue(vertex.getMeta(), Map.class);
                    dto.setMeta(map);
                } catch (JsonProcessingException e) {
                    log.error("Failed reading vertex meta value: {} Reason: {}", vertex.getMeta(), e.getMessage());
                }

                return dto;
            }

            @Override
            protected Vertex doBackward(VertexDTO vertexDTO) {
                Vertex vertex = modelMapper.map(vertexDTO, Vertex.class);
                return vertex;
            }
        };
    }


}
