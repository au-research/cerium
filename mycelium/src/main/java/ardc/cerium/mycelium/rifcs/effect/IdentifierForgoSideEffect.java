package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class IdentifierForgoSideEffect extends SideEffect implements Serializable {

    private final String registryObjectId;
    // The Identifier that relationships to be removed from
    private final String deletedIdentifier;
    // the title of the record (need it when removing it from portal index
    private final String searchTitle;
    // record class and type is needed to find where to remove the title from the portal index
    // eg: related_party_multi [title1, title2]
    // the title of the record
    private final String recordClass;
    // the title of the record
    private final String recordType;

}