package ardc.cerium.mycelium.rifcs.effect;

import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PrimaryKeyDeletionSideEffect extends SideEffect {

	private final String dataSourceId;

	private final String key;

	private final String registryObjectId;

}
