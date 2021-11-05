package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyDeletionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.DataSourceUtil;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class PrimaryKeyDeletionExecutor extends Executor {

	private final PrimaryKeyDeletionSideEffect sideEffect;

	public PrimaryKeyDeletionExecutor(PrimaryKeyDeletionSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link PrimaryKeyDeletionSideEffect} is applicable
	 * @param before the {@link DataSource} state before the mutation
	 * @param after the {@link DataSource} state after the mutation
	 * @param myceliumService the {@link MyceliumService} to access services
	 * @return true if the DataSource has additional primaryKey in the settings
	 */
	public static boolean detect(DataSource before, DataSource after, MyceliumService myceliumService) {
		List<PrimaryKey> differences = DataSourceUtil.getPrimaryKeyDifferences(after, before);

		return differences.size() > 0;
	}

	/**
	 * Detect if {@link PrimaryKeyDeletionSideEffect} is applicable
	 * @param before the {@link RecordState} before RegistryObject mutation
	 * @param after the {@link RecordState} after RegistryObject mutation
	 * @param myceliumService the {@link MyceliumService} to access services
	 * @return true if the RegistryObject is deleted and was a PrimaryKey
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {

		// registryObject is deleted, after has to be null
		if (after != null) {
			return false;
		}

		// check if the before key is any of the dataSource's primaryKey
		DataSource dataSource = myceliumService.getDataSourceById(before.getDataSourceId());
		return dataSource != null && dataSource.getPrimaryKeySetting().getPrimaryKeys().stream()
				.anyMatch(primaryKey -> primaryKey.getKey().equals(before.getRegistryObjectKey()));
	}

	@Override
	public void handle() {

		// remove all PrimaryKey edge to the key
		String key = sideEffect.getKey();
		getMyceliumService().getGraphService().deletePrimaryKeyEdge(key);

		// remove all PrimaryKey edge from/to the roVertex
		String registryObjectId = sideEffect.getRegistryObjectId();
		getMyceliumService().getMyceliumIndexingService().deletePrimaryKeyEdges(registryObjectId);

		// handle deletion of extra edges in SOLR when the relationType deleted is a
		// GrantsNetwork
		String toClass = sideEffect.getRegistryObjectClass();
		String relationType = sideEffect.getRelationType();
		String dataSourceId = sideEffect.getDataSourceId();
		GraphService graphService = getMyceliumService().getGraphService();
		MyceliumIndexingService myceliumIndexingService = getMyceliumService().getMyceliumIndexingService();
		if (RelationUtil.isGrantsNetwork("collection", toClass, relationType)) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSourceId, "collection")) {
				stream.forEach(myceliumIndexingService::regenGrantsNetworkRelationships);
			}
		}
		if (RelationUtil.isGrantsNetwork("activity", toClass, relationType)) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSourceId, "activity")) {
				stream.forEach(myceliumIndexingService::regenGrantsNetworkRelationships);
			}
		}
		if (RelationUtil.isGrantsNetwork("party", toClass, relationType)) {
			try (Stream<Vertex> stream = graphService.streamRegistryObjectFromDataSource(dataSourceId, "party")) {
				stream.forEach(myceliumIndexingService::regenGrantsNetworkRelationships);
			}
		}
	}

}
