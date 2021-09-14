package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.GrantsNetworkForgoSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
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
		MyceliumIndexingService myceliumIndexingService = myceliumService.getMyceliumIndexingService();

		Vertex vertex = myceliumService.getVertexFromRegistryObjectId(sideEffect.getRegistryObjectId());
		if (vertex == null) {
			log.debug("RegistryObjectID vertex is not found, attempting to find KeyVertex");
			vertex = myceliumService.getGraphService().getVertexByIdentifier(sideEffect.registryObjectKey,
					RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		}

		if (vertex == null) {
			log.error("KeyVertex is also not found for RegistryObjec[id={}, key={}]", sideEffect.getRegistryObjectId(),
					sideEffect.getRegistryObjectKey());
			return;
		}

		log.debug("Found source Vertex[id={}, type={}]", vertex.getIdentifier(), vertex.getIdentifierType());

		// for collection, all child collections need reindexing of grantsNetwork
		// for activity, all child activities, child collections need reindexing of
		// grantsNetwork
		// for party, does not need to do anything
		String registryObjectClass = sideEffect.getRegistryObjectClass();
		log.debug("Indexing Affected Relationships for RegistryObject[class={}]", registryObjectClass);
		if (registryObjectClass.equals("collection")) {
			// index GrantsNetwork for each child collection
			try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildCollection(vertex)) {
				stream.forEach(collection -> {
//					myceliumIndexingService.indexVertex(collection);
					myceliumIndexingService.deleteGrantsNetworkEdges(collection);
					myceliumIndexingService.indexImplicitLinksForCollection(collection);
				});
			}
		}
		else if (registryObjectClass.equals("activity")) {
			try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildActivity(vertex)) {
				stream.forEach(activity -> {
//					myceliumIndexingService.indexVertex(activity);
					myceliumIndexingService.deleteGrantsNetworkEdges(activity);
					myceliumIndexingService.indexImplicitLinksForActivity(activity);
				});
			}
			try (Stream<Vertex> stream = myceliumService.getGraphService().streamChildCollection(vertex)) {
				stream.forEach(collection -> {
//					myceliumIndexingService.indexVertex(collection);
					myceliumIndexingService.deleteGrantsNetworkEdges(collection);
					myceliumIndexingService.indexImplicitLinksForCollection(collection);
				});
			}
		}
		log.info("Finished handling GrantsNetworkForgoExecutor");
	}

}
