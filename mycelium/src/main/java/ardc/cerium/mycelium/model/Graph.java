package ardc.cerium.mycelium.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Graph {

	private List<Vertex> vertices = new ArrayList<>();

	private List<Edge> edges = new ArrayList<>();

	public void addVertex(Vertex vertex) {
		if (!this.vertices.contains(vertex)) {
			this.vertices.add(vertex);
		}
	}

	public void removeVertex(Vertex vertex) {
		this.vertices.remove(vertex);
	}

	public void removeAll(Collection<Vertex> vertices) {
		this.vertices.removeAll(vertices);
	}


	public void addVertex(Vertex... vertices) {
		this.vertices.addAll(Arrays.asList(vertices));
	}

	public void addEdge(Edge edge) {
		if (!this.edges.contains(edge)) {
			this.edges.add(edge);
		}
	}

	/**
	 * Merge with another {@link Graph} results in the inclusion of all vertices and edges
	 * of the other graph. Uniqueness is handled internally
	 * @param graph the {@link Graph} to merge with
	 */
	public void mergeGraph(Graph graph) {
		graph.getVertices().forEach(this::addVertex);
		graph.getEdges().forEach(this::addEdge);
	}

}
