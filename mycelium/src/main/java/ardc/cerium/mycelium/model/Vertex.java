package ardc.cerium.mycelium.model;

import lombok.*;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Vertex {

	@Property("identifier")
	private String identifier;

	@Property("identifierType")
	private String identifierType;

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

	@Property("listTitle")
	private String listTitle;

	@Property("group")
	private String group;

	@Property("dataSourceId")
	private String dataSourceId;

	@Property("notes")
	private String notes;

	@Property("createdAt")
	private Date createdAt;

	@Property("updatedAt")
	private Date updatedAt;

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
		Vertex, RegistryObject, Identifier, DataSource, Cluster
	}

}
