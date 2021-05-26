package ardc.cerium.mycelium.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Edge {

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

	public Edge(Vertex from, Vertex to, String type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

}
