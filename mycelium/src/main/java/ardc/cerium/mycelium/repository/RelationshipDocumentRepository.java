package ardc.cerium.mycelium.repository;

import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.stream.Stream;

public interface RelationshipDocumentRepository extends SolrCrudRepository<RelationshipDocument, String> {

    void deleteAllByFromIdEquals(String fromId);

    void deleteAllByToIdentifierEquals(String toIdentifier);

    void deleteAllByFromIdIn(List<String> fromIds);

    void deleteAllByToIdentifierIn(List<String> fromIds);

    void deleteAllByFromDataSourceId(String dataSourceId);

    void deleteAllByToDataSourceId(String dataSourceId);

    RelationshipDocument findRelationshipDocumentByFromIdAndToIdentifierAndToIdentifierType(String fromId, String toIdentifier, String toIdentifierType);

    @Query(fields = { "[child parentFilter=type:relationship]", "*"})
    Streamable<RelationshipDocument> streamRelationshipDocumentByFromId(String fromId);

    @Query(fields = { "[child parentFilter=type:relationship]", "*"})
    Streamable<RelationshipDocument> streamRelationshipDocumentByToIdentifier(String toIdentifier);
}
