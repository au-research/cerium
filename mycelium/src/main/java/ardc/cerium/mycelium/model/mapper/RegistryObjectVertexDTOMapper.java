package ardc.cerium.mycelium.model.mapper;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.RegistryObjectVertexDTO;
import clover.com.google.common.base.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@Getter
public class RegistryObjectVertexDTOMapper {

    final ModelMapper modelMapper;

    final ObjectMapper objectMapper;

    protected Converter<Vertex, RegistryObjectVertexDTO> converter;


    public RegistryObjectVertexDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.converter = buildConverter();
        this.objectMapper = new ObjectMapper();
    }

    private Converter<Vertex, RegistryObjectVertexDTO> buildConverter()
    {
        return new Converter<Vertex, RegistryObjectVertexDTO>() {
            @Override
            protected RegistryObjectVertexDTO doForward(Vertex vertex) {
                RegistryObjectVertexDTO dto = new RegistryObjectVertexDTO();

                dto.setRegistryObjectId(vertex.getIdentifier());
                dto.setObjectType(vertex.getObjectType());
                dto.setObjectClass(vertex.getObjectClass());
                dto.setCreatedAt(vertex.getCreatedAt());
                dto.setUpdatedAt(vertex.getCreatedAt());
                dto.setStatus(vertex.getStatus());
                dto.setDataSourceId(vertex.getDataSourceId());

                try {
                    Map<String, Object> map = objectMapper.readValue(vertex.getMeta(), Map.class);
                    dto.setMeta(map);
                } catch (JsonProcessingException e) {
                    log.error("Failed reading vertex meta value: {} Reason: {}", vertex.getMeta(), e.getMessage());
                }

                return dto;
            }

            @Override
            protected Vertex doBackward(RegistryObjectVertexDTO registryObjectVertexDTO) {
                // not implemented
                return null;
            }
        };
    }
}
