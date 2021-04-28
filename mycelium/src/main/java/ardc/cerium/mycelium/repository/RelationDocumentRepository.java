package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.RelationDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.util.UUID;

public interface RelationDocumentRepository extends SolrCrudRepository<RelationDocument, UUID> {

}
