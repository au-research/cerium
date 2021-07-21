package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

public interface RelationshipDocumentRepository extends SolrCrudRepository<RelationshipDocument, String> {

    RelationshipDocument findRelationshipDocumentByFromIdAndToIdentifierAndToIdentifierType(String fromId, String toIdentifier, String toIdentifierType);
}
