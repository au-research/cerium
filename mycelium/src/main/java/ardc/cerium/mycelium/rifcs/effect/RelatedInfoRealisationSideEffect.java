package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RelatedInfoRealisationSideEffect extends SideEffect {

	private final String registryObjectId;
	private final String identifier;
	private final String identifierType;

	private final String title;
	// record class and type is needed to find where to add the title in the portal index
	// eg: related_party_multi [title1, title2]
	// the title of the record
	private final String recordClass;
	// the title of the record
	private final String recordType;

}
