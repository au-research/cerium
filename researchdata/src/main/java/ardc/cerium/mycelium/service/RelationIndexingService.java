package ardc.cerium.researchdata.service;

import ardc.cerium.researchdata.model.RelationDocument;
import ardc.cerium.researchdata.repository.RelationDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class RelationIndexingService {
	
	private static final Logger log = LoggerFactory.getLogger(RelationIndexingService.class);

	@Autowired
	RelationDocumentRepository relationDocumentRepository;

	public void indexRelations(Collection<RelationDocument> relations) {
		relations.forEach(this::indexRelation);
	}

	public void indexRelation(RelationDocument relationDocument) {
		log.info("Indexing {}", relationDocument);
		relationDocumentRepository.save(relationDocument);
	}

}
