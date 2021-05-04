package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.RelationDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class RelationIndexingService {
	
	private static final Logger log = LoggerFactory.getLogger(RelationIndexingService.class);


	public void indexRelations(Collection<RelationDocument> relations) {
		relations.forEach(this::indexRelation);
	}

	public void indexRelation(RelationDocument relationDocument) {
		log.info("Indexing {}", relationDocument);
	}

}
