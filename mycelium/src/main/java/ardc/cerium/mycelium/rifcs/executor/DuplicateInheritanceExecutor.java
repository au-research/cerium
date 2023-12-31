package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DuplicateInheritanceExecutor extends Executor {

	private final DuplicateInheritanceSideEffect sideEffect;

	private final GraphService graphService;

	private final MyceliumIndexingService indexingService;

	public DuplicateInheritanceExecutor(DuplicateInheritanceSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
		this.graphService = myceliumService.getGraphService();
		this.indexingService = myceliumService.getIndexingService();
	}

	/**
	 * Detect if {@link DuplicateInheritanceSideEffect} is applicable
	 *
	 * A record is considered to have duplicate inheritance is when it is created, and
	 * brings with it inheritable relationships to its duplicates
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @param myceliumService the {@link MyceliumService} for additional business logic
	 * @return boolean
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {

		// only applies for record creation
		// todo investigate if this is necessary/applicable?
		if (before != null) {
			return false;
		}

		// if the afterStates have duplicate registryObject
		Vertex afterVertex = myceliumService.getVertexFromRegistryObjectId(after.getRegistryObjectId());
		Collection<Vertex> myDuplicates = myceliumService.getGraphService().getSameAsRegistryObject(afterVertex)
				.stream().filter(v -> !v.getIdentifier().equals(afterVertex.getIdentifier()))
				.collect(Collectors.toList());
		if (myDuplicates.size() == 0) {
			// only applicable if the record has duplicates
			return false;
		}

		Collection<Relationship> directOutbounds = myceliumService.getGraphService()
				.getDirectOutboundRelationships(afterVertex.getIdentifier(), afterVertex.getIdentifierType());
		// only applicable if the record has direct outbound links
		return directOutbounds.size() != 0;

	}

	@Override
	public void handle() {
		String registryObjectId = sideEffect.getAffectedRegistryObjectId();

		Vertex origin = graphService.getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (origin == null) {
			// todo log to request -> unknown registryObjectId
			return;
		}

		// find all duplicates of origin
		Collection<Vertex> myDuplicates = graphService.getSameAsRegistryObject(origin).stream()
				.filter(v -> !v.getIdentifier().equals(registryObjectId)).collect(Collectors.toList());
		if (myDuplicates.size() == 0) {
			// todo log to request -> no duplicates found
			return;
		}

		// find all directly related vertices
		Collection<Relationship> directOutbounds = graphService.getDirectOutboundRelationships(origin.getIdentifier(),
				origin.getIdentifierType());
		if (directOutbounds.size() == 0) {
			// todo log to request -> no outbounds found
			return;
		}

		// ensure (dupe)-[r]->(related) and (dupe)<-[rr]-(related) exists in Neo4j and
		// SOLR
		directOutbounds.forEach(outbound -> {
			Vertex to = outbound.getTo();

			myDuplicates.forEach(dupe -> {
				indexingService.indexRelation(dupe, to, outbound.getRelations());

				List<EdgeDTO> reversedEdges = outbound.getRelations().stream().map(RelationUtil::getReversed)
						.collect(Collectors.toList());
				indexingService.indexRelation(to, dupe, reversedEdges);
			});
		});
	}

}
