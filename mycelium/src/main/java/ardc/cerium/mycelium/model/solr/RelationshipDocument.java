package ardc.cerium.mycelium.model.solr;


import lombok.Data;
import org.springframework.data.solr.core.mapping.ChildDocument;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Date;
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

    @Indexed(name = "from_key")
    private String fromKey;

    @Indexed(name="from_title")
    private String fromTitle;

    @Indexed(name="from_list_title")
    private String fromListTitle;

    @Indexed(name="from_class")
    private String fromClass;

    @Indexed(name="from_type")
    private String fromType;

    @Indexed(name="from_group")
    private String fromGroup;

    @Indexed(name="from_url")
    private String fromUrl;

    @Indexed(name="from_notes")
    private String fromNotes;

    @Indexed(name="from_data_source_id")
    private String fromDataSourceId;

    @Indexed(name="from_status")
    private String fromStatus;

    @Indexed(name = "to_identifier")
    private String toIdentifier;

    @Indexed(name = "to_key")
    private String toKey;

    @Indexed(name = "to_identifier_type")
    private String toIdentifierType;

    @Indexed(name="to_title")
    private String toTitle;

    @Indexed(name="to_list_title")
    private String toListTitle;

    @Indexed(name="to_class")
    private String toClass;

    @Indexed(name="to_type")
    private String toType;

    @Indexed(name="to_group")
    private String toGroup;

    @Indexed(name="to_url")
    private String toUrl;

    @Indexed(name="to_notes")
    private String toNotes;

    @Indexed(name="to_data_source_id")
    private String toDataSourceId;

    @Indexed(name="to_status")
    private String toStatus;

    @Indexed(name="created_at")
    private Date createdAt;

    @Indexed(name="updated_at")
    private Date updatedAt;

    @ChildDocument
    List<EdgeDocument> relations = new ArrayList<>();
}
