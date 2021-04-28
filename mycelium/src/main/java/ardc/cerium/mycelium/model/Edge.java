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

	private Origin origin = Origin.Explicit;

	public Edge(Vertex from, Vertex to, String type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	public enum Origin {

		Explicit, Implicit

	}

}
