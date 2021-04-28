package ardc.cerium.mycelium.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class Vertex {

	@Property("identifier")
	private final String identifier;

	@Property("identifierType")
	private final String identifierType;

	@DynamicLabels
	private List<String> labels;

	private String type;

	private String classification;

	private int registryObjectId;

	// model DataSource as a Relation?
	private String dataSourceKey;

	private Status status = Status.Published;

	@Id
	@GeneratedValue
	private Long id;

	public Vertex(String identifier, String identifierType) {
		this.identifier = identifier;
		this.identifierType = identifierType;
		labels = new ArrayList<>();
	}

	public void addLabel(Label label) {
		labels.add(label.toString());
	}

    public boolean hasLabel(Label label) {
		return labels.contains(label.toString());
	}

	public enum Label {
		Vertex, RegistryObject, Identifier
	}

	public enum Status {
		Published, Draft
	}

}
