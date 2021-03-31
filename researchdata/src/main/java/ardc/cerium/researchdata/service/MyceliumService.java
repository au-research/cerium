package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.provider.RIFCSGraphProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class MyceliumService {

	@Autowired
	GraphService graphService;

	// @Autowired
	// RelationIndexingService relationIndexingService;

	public void ingest(String payload) {

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(payload);

		// insert into neo4j graph
		graphService.ingestGraph(graph);

		// todo consider if indexing to relations core is even required
		// todo fix indexing graph with originNode in mind
		// todo queue
		// Vertex origin = graph.getOriginNode();
		// int limit = 100;
		// int offset = 0;
		// Collection<RelationDocument> batch =
		// graphService.getRelationships(origin.getIdentifier(),
		// origin.getIdentifierType(), limit, offset);
		//
		// while (!batch.isEmpty()) {
		// relationIndexingService.indexRelations(batch);
		// log.info("Indexed {} relations", batch.size());
		// offset += limit;
		// batch = graphService.getRelationships(origin.getIdentifier(),
		// origin.getIdentifierType(), limit, offset);
		// }
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
