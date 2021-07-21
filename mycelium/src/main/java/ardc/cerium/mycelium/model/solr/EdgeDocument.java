package ardc.cerium.mycelium.model.solr;

import lombok.Data;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import javax.persistence.Id;
import java.util.UUID;

@SolrDocument(collection = "relationships")
@Data
public class EdgeDocument{

    @Id
    @Indexed(name = "id", type = "string")
    private String id;

    @Indexed(name="type", type="string")
    private String type = "edge";

    @Indexed(name = "relation_type", type="string")
    private String relationType;

    @Indexed(name = "relation_origin", type="string")
    private String relationOrigin;

    @Indexed(name = "relation_notes", type="string")
    private String relationNotes;

    @Indexed(name = "relation_reverse", type="boolean")
    private boolean relationReverse;

    @Indexed(name = "relation_internal", type="boolean")
    private boolean relationInternal;

    public EdgeDocument(String relationType) {
        this.id = UUID.randomUUID().toString();
        this.relationType = relationType;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof EdgeDocument){
            return relationType.equals(((EdgeDocument) o).getRelationType());
        }
        return false;
    }
}
