package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.GrantsNetworkForgoSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import ardc.cerium.mycelium.util.RelationUtil;

import java.util.List;

public class GrantsNetworkForgoExecutor extends Executor {

	private final GrantsNetworkForgoSideEffect sideEffect;

	public GrantsNetworkForgoExecutor(GrantsNetworkForgoSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link GrantsNetworkForgoSideEffect} is applicable
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @param myceliumService the {@link MyceliumService} for additional business logic
	 * @return boolean
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
		// the record is deleted and the Before state relationships contain grants network
		// relations
		if (after != null && before.getOutbounds().stream().anyMatch(RelationUtil::isGrantsNetwork)) {
			return true;
		}

		// the record is updated, and the relations differences contain grants network
		// relations
		List<Relationship> differences = RelationUtil.getRelationshipsDifferences(before, after);
		return differences.stream().anyMatch(RelationUtil::isGrantsNetwork);
	}

	@Override
	public void handle() {
		MyceliumService myceliumService = getMyceliumService();

		Vertex vertex = myceliumService.getVertexFromRegistryObjectId(sideEffect.getRegistryObjectId());

		// obtain from SOLR
		// for collection, all child collections need reindexing of grantsNetwork
		// for activity, all child activities and child collections need reindexing of
		// grantsNetwork
		// for party, does not need to do anything
	}

}
