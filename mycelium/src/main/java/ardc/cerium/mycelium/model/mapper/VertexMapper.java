package ardc.cerium.mycelium.model.mapper;

import ardc.cerium.mycelium.model.Vertex;
import com.google.common.base.Converter;
import lombok.Getter;
import org.modelmapper.ModelMapper;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Getter
public class VertexMapper {

	final ModelMapper modelMapper;

	protected Converter<Node, Vertex> converter;

	public VertexMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
		this.converter = buildConverter();
	}

	private Converter<Node, Vertex> buildConverter() {
		return new Converter<>() {
			@Override
			protected Vertex doForward(Node node) {
				Vertex vertex = new Vertex(node.get("identifier").asString(), node.get("identifierType").asString());
				vertex.setLabels(StreamSupport.stream(node.labels().spliterator(), false).collect(Collectors.toList()));
				vertex.setId(node.id());

				if (!node.get("objectClass").isNull()) {
					vertex.setObjectClass(node.get("objectClass").asString());
				}
				if (!node.get("objectType").isNull()) {
					vertex.setObjectType(node.get("objectType").asString());
				}
				if (!node.get("visible").isNull()) {
					vertex.setVisible(node.get("visible").asBoolean());
				}
				if (!node.get("url").isNull()) {
					vertex.setUrl(node.get("url").asString());
				}

				return vertex;
			}

			@Override
			protected Node doBackward(Vertex vertex) {
				// not implemented
				return null;
			}
		};
	}

}
