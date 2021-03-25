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

	private static final Logger log = LoggerFactory.getLogger(MyceliumService.class);

	@Autowired
	GraphService graphService;

	@Autowired
	RelationIndexingService relationIndexingService;

	public void ingest(String xml) {

		// supports rifcs for now
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(xml);

		// insert into neo4j graph
		graphService.ingestGraph(graph);

		// todo queue indexing originNode
		Vertex origin = graph.getOriginNode();
		int limit = 100;
		int offset = 0;
		Collection<RelationDocument> batch = graphService.getRelationships(origin.getIdentifier(),
				origin.getIdentifierType(), limit, offset);

		while (!batch.isEmpty()) {
			relationIndexingService.indexRelations(batch);
			log.info("Indexed {} relations", batch.size());
			offset += limit;
			batch = graphService.getRelationships(origin.getIdentifier(), origin.getIdentifierType(), limit, offset);
		}
	}

}
