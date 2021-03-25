package ardc.cerium.researchdata.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Edge {

	private final String type;

	Vertex from;

	Vertex to;

	private String description;

	private String url;

	public Edge(Vertex from, Vertex to, String type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

}
