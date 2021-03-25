package ardc.cerium.researchdata.repository;

import ardc.cerium.researchdata.model.RelationDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.util.UUID;

public interface RelationDocumentRepository extends SolrCrudRepository<RelationDocument, UUID> {

}
