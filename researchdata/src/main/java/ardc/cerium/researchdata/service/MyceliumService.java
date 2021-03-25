package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.model.Graph;
import ardc.cerium.researchdata.model.RelationDocument;
import ardc.cerium.researchdata.model.Vertex;
import ardc.cerium.researchdata.provider.RIFCSGraphProvider;
import ardc.cerium.researchdata.repository.RelationDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class MyceliumService {

	@Autowired
	GraphService graphService;

	@Autowired
	RelationIndexingService relationIndexingService;

	public void ingest(String payload) {

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(payload);

		// insert into neo4j graph
		graphService.ingestGraph(graph);

		// todo consider if indexing to relations core is even required
		// todo fix indexing graph with originNode in mind
		// todo queue
//		Vertex origin = graph.getOriginNode();
//		int limit = 100;
//		int offset = 0;
//		Collection<RelationDocument> batch = graphService.getRelationships(origin.getIdentifier(),
//				origin.getIdentifierType(), limit, offset);
//
//		while (!batch.isEmpty()) {
//			relationIndexingService.indexRelations(batch);
//			log.info("Indexed {} relations", batch.size());
//			offset += limit;
//			batch = graphService.getRelationships(origin.getIdentifier(), origin.getIdentifierType(), limit, offset);
//		}
	}

}
