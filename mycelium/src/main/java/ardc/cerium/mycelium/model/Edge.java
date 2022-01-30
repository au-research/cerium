package ardc.cerium.mycelium.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;

import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode
public class Edge {

	@Id
	@GeneratedValue
	private Long id;

	private final String type;

	Vertex from;

	Vertex to;

	private String description;

	private String url;


	// todo consider making this into enumeration
	// possible values: Direct, Duplicate, GrantsNetwork, PrimaryKey
	private String origin = "Direct";

	private boolean isReverse = false;

	private boolean isDuplicate = false;

	private boolean isPublic = true;

	private boolean isInternal = true;

	private Date createdAt;

	private Date updatedAt;

	public Edge(Vertex from, Vertex to, String type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	public Edge(Vertex from, Vertex to,  String type, Long id) {
		this.id = id;
		this.type = type;
		this.from = from;
		this.to = to;
	}
}
