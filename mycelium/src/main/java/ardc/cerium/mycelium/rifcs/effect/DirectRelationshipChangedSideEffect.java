package ardc.cerium.mycelium.rifcs.effect;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DirectRelationshipChangedSideEffect extends SideEffect {

    public String registryObjectId;

    public String relatedObjectId;
    // delete || add
    public String action;

    // record class and type is needed to find where to add the title in the portal index
    // eg: related_party_multi [title1, title2]
    // the class of the record
    private final String recordClass;
    // the type of the record
    private final String recordType;

    private final String title;

    private final String relationshipType;

}
