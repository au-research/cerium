package ardc.cerium.mycelium.model.solr;


import lombok.Data;
import org.springframework.data.solr.core.mapping.ChildDocument;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import javax.persistence.Id;
import java.util.List;

@SolrDocument(collection = "relationships")
@Data
public class RelationshipDocument {

    @Id
    @Indexed(name = "id", type = "string")
    private String id;

    @Indexed(name="type", type="string")
    private String type = "relationship";

    @Indexed(name = "from_id")
    private String fromId;

    @Indexed(name="from_title")
    private String fromTitle;

    @Indexed(name="from_class")
    private String fromClass;

    @Indexed(name="from_type")
    private String fromType;

    @Indexed(name = "to_identifier")
    private String toIdentifier;

    @Indexed(name = "to_identifier_type")
    private String toIdentifierType;

    @Indexed(name="to_title")
    private String toTitle;

    @Indexed(name="to_class")
    private String toClass;

    @Indexed(name="to_type")
    private String toType;
    

    @ChildDocument
    List<EdgeDocument> relations;
}