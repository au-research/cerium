package ardc.cerium.researchdata.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node
@Getter
@Setter
public class Vertex {

	@Id
	@GeneratedValue
	private Long id;

	@Property("identifier")
	private final String identifier;

	@Property("identifierType")
	private final String identifierType;

	public Vertex(String identifier, String identifierType) {
		this.identifier = identifier;
		this.identifierType = identifierType;
	}

}
