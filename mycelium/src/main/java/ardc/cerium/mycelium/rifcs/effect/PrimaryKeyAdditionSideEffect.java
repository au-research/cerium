package ardc.cerium.mycelium.rifcs.effect;

import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PrimaryKeyAdditionSideEffect extends SideEffect {

	private final String dataSourceId;

	private final PrimaryKey primaryKey;

}
