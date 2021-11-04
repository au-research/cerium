package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.GrantsNetworkInheritenceSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;

import java.util.List;
import java.util.stream.Stream;

public class GrantsNetworkInheritenceExecutor extends Executor {

	private final GrantsNetworkInheritenceSideEffect sideEffect;

	public GrantsNetworkInheritenceExecutor(GrantsNetworkInheritenceSideEffect sideEffect,
			MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	/**
	 * Detect if {@link GrantsNetworkInheritenceSideEffect} is applicable
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @param myceliumService the {@link MyceliumService} for additional business logic
	 * @return boolean
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
		// the record is created
		// and the after state relationships contain grants network relations
		if (before == null && after.getOutbounds().stream().anyMatch(RelationUtil::isGrantsNetwork)) {
			return true;
		}

		// the record is updated
		// and the relations differences contain grants network relations
		List<Relationship> differences = RelationUtil.getRelationshipsDifferences(after, before);
		return differences.stream().anyMatch(RelationUtil::isGrantsNetwork);
	}

	@Override
	public void handle() {
		MyceliumService myceliumService = getMyceliumService();
		MyceliumIndexingService myceliumIndexingService = myceliumService.getMyceliumIndexingService();
		String registryObjectClass = sideEffect.getRegistryObjectClass();

		Vertex vertex = myceliumService.getVertexFromRegistryObjectId(sideEffect.getRegistryObjectId());

		switch (registryObjectClass) {
			case "collection":
				myceliumIndexingService.indexImplicitLinksForCollection(vertex);
				try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildCollection(vertex)) {
					stream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
				}
				break;
			case "activity":
				myceliumIndexingService.indexImplicitLinksForActivity(vertex);

				// going down the tree
				try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildActivity(vertex)) {
					stream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
				}
				try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildCollection(vertex)) {
					stream.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
				}

				// going up the tree and then down
				try (Stream<Vertex> stream = myceliumService.getGraphService().streamParentActivity(vertex)) {
					stream.forEach(parentActivity -> {
						myceliumIndexingService.indexGrantsNetworkRelationships(parentActivity);
						try (Stream<Vertex> parentsCollectionChildrenStream = myceliumService.getGraphService()
								.streamChildCollection(parentActivity)) {
							parentsCollectionChildrenStream
									.forEach(myceliumIndexingService::indexGrantsNetworkRelationships);
						}
					});
				}

				break;
			case "party":
				myceliumIndexingService.indexImplicitLinksForParty(vertex);
				break;
		}
	}

}
