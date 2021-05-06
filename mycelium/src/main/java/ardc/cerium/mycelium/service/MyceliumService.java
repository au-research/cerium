package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class MyceliumService {

	private final GraphService graphService;

	public MyceliumService(GraphService graphService) {
		this.graphService = graphService;
	}

	public void ingest(String payload) {

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(payload);

		// insert into neo4j graph
		graphService.ingestGraph(graph);

		// todo reverse links generations
		// todo implicit links generations
	}

	/**
	 * Finds a {@link Collection<Vertex>} of RegistryObject that is considered identical.
	 * Identical Registry object shares the same Identifier (isSameAs to the same
	 * Identifier). This property is transitive
	 * @param origin the {@link Vertex} to start the search in
	 * @return a {@link Collection<Vertex>} that contains all the identical {@link Vertex}
	 */
	public Collection<Vertex> getDuplicateRegistryObject(Vertex origin) {
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());

		// only return the RegistryObject
		return sameAsNodeCluster.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.collect(Collectors.toList());
	}

}
