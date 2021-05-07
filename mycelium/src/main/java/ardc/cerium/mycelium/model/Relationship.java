package ardc.cerium.mycelium.model;

import ardc.cerium.mycelium.model.dto.EdgeDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Relationship {

	private Vertex from;

	private Vertex to;

	private List<EdgeDTO> relations;

}
