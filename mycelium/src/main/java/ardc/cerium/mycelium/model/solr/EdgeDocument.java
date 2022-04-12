package ardc.cerium.mycelium.model.solr;

import lombok.Data;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@SolrDocument(collection = "relationships")
@Data
public class EdgeDocument{

    @Id
    @Indexed(name = "id", type = "string")
    private String id;

    @Indexed(name="from_id", type="string")
    private String fromId;

    @Indexed(name="to_identifier", type="string")
    private String toIdentifier;

    @Indexed(name="type", type="string")
    private String type = "edge";

    @Indexed(name = "relation_type", type="string")
    private String relationType;

    @Indexed(name = "relation_type_text", type = "string")
    private String relationTypeText;

	@Indexed(name = "relation_origin", type = "string")
	private String relationOrigin;

    @Indexed(name = "relation_reverse", type="boolean")
    private boolean relationReverse;

    @Indexed(name = "relation_internal", type="boolean")
    private boolean relationInternal;

    @Indexed(name = "relation_description", type="string")
    private String relationDescription;

    @Indexed(name = "relation_url", type="string")
    private String relationUrl;

    @Indexed(name="from_data_source_id", type="string")
    private String fromDataSourceId;

    @Indexed(name="from_status", type="string")
    private String fromStatus;

    @Indexed(name="to_status", type="string")
    private String toStatus;

    @Indexed(name="created_at")
    private Date createdAt;

    @Indexed(name="updated_at")
    private Date updatedAt;


    public EdgeDocument(String relationType) {
        this.id = UUID.randomUUID().toString();
        this.relationType = relationType;
    }

    @Override
    public boolean equals(Object o) {
		if (o instanceof EdgeDocument) {
		    EdgeDocument other = (EdgeDocument) o;
			return relationType.equals(other.getRelationType())
					&& relationOrigin.equals(other.getRelationOrigin());
		}
		return false;
	}
}
