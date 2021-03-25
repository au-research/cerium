package ardc.cerium.researchdata.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.SolrDocument;

import javax.persistence.Id;
import java.util.UUID;

@SolrDocument(collection = "relations")
@Getter
@Setter
public class RelationDocument {

    @Id
    private UUID id;

    @Field
    private String fromIdentifier;

    @Field
    private String fromIdentifierType;

    @Field
    private String toIdentifier;

    @Field
    private String toIdentifierType;

    @Field
    private String relationType;

    @Field
    private String origin;

}
