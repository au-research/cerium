package ardc.cerium.mycelium.model.mapper;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.TreeNodeDTO;
import lombok.Getter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.google.common.base.Converter;

@Service
@Getter
public class TreeNodeDTOMapper {
    final ModelMapper modelMapper;

    protected Converter<Vertex, TreeNodeDTO> converter;

    public TreeNodeDTOMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.converter = buildConverter();
    }

    private Converter<Vertex, TreeNodeDTO> buildConverter() {
        return new Converter<>() {

            @Override
            protected TreeNodeDTO doForward(Vertex vertex) {
                TreeNodeDTO dto = new TreeNodeDTO();
                dto.setIdentifier(String.valueOf(vertex.getIdentifier()));
                dto.setIdentifierType(vertex.getIdentifierType());
                dto.setTitle(vertex.getTitle());
                dto.setObjectClass(vertex.getObjectClass());
                dto.setObjectType(vertex.getObjectType());
                dto.setUrl(vertex.getUrl());
                return dto;
            }

            @Override
            protected Vertex doBackward(TreeNodeDTO treeNodeDTO) {
                // not implemented
                return null;
            }
        };
    }
}
