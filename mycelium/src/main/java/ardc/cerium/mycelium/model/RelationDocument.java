package ardc.cerium.mycelium.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class RelationDocument {

    @Id
    private UUID id;

    private String fromIdentifier;

    private String fromIdentifierType;

    private String toIdentifier;

    private String toIdentifierType;

    private String relationType;

    private String origin;

    private String url;

    private String description;

    private Date createdAt;

    private Date updatedAt;

}
