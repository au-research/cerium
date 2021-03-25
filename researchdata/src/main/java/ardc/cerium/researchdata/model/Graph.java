package ardc.cerium.researchdata.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Graph {

	private List<Vertex> vertices = new ArrayList<>();

	private List<Edge> edges = new ArrayList<>();

	private Vertex originNode;

	public void addVertex(Vertex vertex) {
		this.vertices.add(vertex);
	}

	public void addEdge(Edge edge) {
		this.edges.add(edge);
	}

}
