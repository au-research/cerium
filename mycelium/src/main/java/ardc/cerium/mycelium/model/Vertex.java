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

	@Property("objectType")
	private String objectType;

	@Property("objectClass")
	private String objectClass;

	@Property("public")
	private boolean isPublic = true;

	@Property("url")
	private String url;

	@Property("title")
	private String title;

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
		Vertex, RegistryObject, Identifier, DataSource
	}

}
