package ardc.cerium.mycelium.rifcs.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PrimaryKeyDeletionSideEffect extends SideEffect {

	private final String dataSourceId;

	private final String key;

	private final String affectedRegistryObjectId;

	private final String registryObjectClass;

	private final String relationType;

}
