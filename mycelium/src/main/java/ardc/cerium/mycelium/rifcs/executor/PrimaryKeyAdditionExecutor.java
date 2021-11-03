package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyAdditionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.DataSourceUtil;

import java.util.List;

public class PrimaryKeyAdditionExecutor extends Executor {

	private final PrimaryKeyAdditionSideEffect sideEffect;

	public PrimaryKeyAdditionExecutor(PrimaryKeyAdditionSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	public static boolean detect(DataSource before, DataSource after) {
		List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(before, after);
		return differences.size() > 0;
	}

	@Override
	public void handle() {
		// this primaryKey should be added to all RegistryObject belongs to this DataSource
		PrimaryKey pk = sideEffect.getPrimaryKey();
		DataSource dataSource = getMyceliumService().getDataSourceById(sideEffect.getDataSourceId());

		// todo ensure the PrimaryKey exists in Neo4j and in SOLR

		// todo handle GrantsNetwork if the PrimaryKey's relationType is a GrantsNetwork edge
	}

}
