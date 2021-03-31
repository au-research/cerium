package ardc.cerium.researchdata.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class Graph {

	private List<Vertex> vertices = new ArrayList<>();

	private List<Edge> edges = new ArrayList<>();

	public void addVertex(Vertex vertex) {
		if (!this.vertices.contains(vertex)) {
			this.vertices.add(vertex);
		}
	}

	public void addVertex(Vertex... vertices) {
		this.vertices.addAll(Arrays.asList(vertices));
	}

	public void addEdge(Edge edge) {
		if (!this.edges.contains(edge)) {
			this.edges.add(edge);
		}
	}

}
