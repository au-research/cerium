package ardc.cerium.mycelium.model.mapper;

import ardc.cerium.mycelium.model.dto.EdgeDTO;
import com.google.common.base.Converter;
import lombok.Getter;
import org.modelmapper.ModelMapper;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

@Service
@Getter
public class EdgeDTOMapper {
    final ModelMapper modelMapper;

    protected Converter<Relationship, EdgeDTO> converter;

    public EdgeDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.converter = buildConverter();
    }

    private Converter<Relationship, EdgeDTO> buildConverter() {
        return new Converter<>() {
            @Override
            protected EdgeDTO doForward(Relationship relationship) {
                EdgeDTO edgeDTO = new EdgeDTO();
                edgeDTO.setType(relationship.type());
                if (!relationship.get("description").isNull()) {
                    edgeDTO.setDescription(relationship.get("description").asString());
                }
                if (!relationship.get("url").isNull()) {
                    edgeDTO.setUrl(relationship.get("url").asString());
                }
                if (!relationship.get("origin").isNull()) {
                    edgeDTO.setOrigin(relationship.get("origin").asString());
                }
                if (!relationship.get("reverse").isNull()) {
                    edgeDTO.setReverse(relationship.get("reverse").asBoolean());
                }
                if (!relationship.get("duplicate").isNull()) {
                    edgeDTO.setDuplicate(relationship.get("duplicate").asBoolean());
                }
                if (!relationship.get("public").isNull()) {
                    edgeDTO.setPublic(relationship.get("public").asBoolean());
                }
                if (!relationship.get("internal").isNull()) {
                    edgeDTO.setInternal(relationship.get("internal").asBoolean());
                }
                return edgeDTO;
            }

            @Override
            protected Relationship doBackward(EdgeDTO edgeDTO) {
                // not implemented
                return null;
            }
        };
    }
}
