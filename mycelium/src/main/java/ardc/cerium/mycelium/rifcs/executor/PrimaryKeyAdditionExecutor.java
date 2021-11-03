package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyAdditionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.DataSourceUtil;

import java.util.List;
import java.util.stream.Collectors;

public class PrimaryKeyAdditionExecutor extends Executor {

	private final PrimaryKeyAdditionSideEffect sideEffect;

	public PrimaryKeyAdditionExecutor(PrimaryKeyAdditionSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link PrimaryKeyAdditionSideEffect} is applicable
	 * @param before the state of the {@link DataSource} before the mutation
	 * @param after the state of the {@link DataSource} after the mutation
	 * @param myceliumService the {@link MyceliumService} to access the graph and services
	 * @return true if there are new PrimaryKey settings added to the DataSource
	 */
	public static boolean detect(DataSource before, DataSource after, MyceliumService myceliumService) {

		// there must be differences
		List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(before, after);

		// the differences (new pk must have resolvable RegistryObject)
		differences = differences.stream().filter(pk -> {
			Vertex registryObjectVertex = myceliumService.getRegistryObjectVertexFromKey(pk.getKey());
			return registryObjectVertex != null;
		}).collect(Collectors.toList());

		return differences.size() > 0;
	}

	@Override
	public void handle() {
		// this primaryKey should be added to all RegistryObject belongs to this
		// DataSource
		PrimaryKey pk = sideEffect.getPrimaryKey();
		DataSource dataSource = getMyceliumService().getDataSourceById(sideEffect.getDataSourceId());

		// todo ensure the PrimaryKey exists in Neo4j and in SOLR

		// todo handle GrantsNetwork if the PrimaryKey relation is a GrantsNetwork edge
	}

}
