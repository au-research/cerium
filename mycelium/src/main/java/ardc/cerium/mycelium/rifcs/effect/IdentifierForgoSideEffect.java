package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class IdentifierForgoSideEffect extends SideEffect implements Serializable {

    private final String affectedRegistryObjectId;
    // The Identifier that relationships to be removed from
    private final String deletedIdentifier;
    // The Identifier Type that relationships to be removed from
    private final String deletedIdentifierType;
    // the title of the record (need it when removing it from portal index
    private final String searchTitle;
    // record class and type is needed to find where to remove the title from the portal index
    // eg: related_party_multi [title1, title2]
    // the class of the record
    private final String recordClass;
    // the type of the record
    private final String recordType;

}