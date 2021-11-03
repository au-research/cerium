package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyDeletionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.DataSourceUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PrimaryKeyDeletionExecutor extends Executor {

	private final PrimaryKeyDeletionSideEffect sideEffect;

	public PrimaryKeyDeletionExecutor(PrimaryKeyDeletionSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	public static boolean detect(DataSource before, DataSource after, MyceliumService myceliumService) {
		List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(after, before);

		return differences.size() > 0;
	}

	@Override
	public void handle() {

		// remove all PrimaryKey edge to the key
		String key = sideEffect.getKey();
		getMyceliumService().getGraphService().deletePrimaryKeyEdge(key);

		// remove all PrimaryKey edge from/to the roVertex
		String registryObjectId = sideEffect.getRegistryObjectId();
		getMyceliumService().getMyceliumIndexingService().deletePrimaryKeyEdges(registryObjectId);

		// todo handle deletion of extra edges in SOLR when the relationType deleted is a GrantsNetwork
	}

}
