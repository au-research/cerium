package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateForgoSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DuplicateForgoExecutor extends Executor {

	private final DuplicateForgoSideEffect sideEffect;

	public DuplicateForgoExecutor(DuplicateForgoSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
	}

	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {

		// if the record is created (it doesn't forgo any duplicates)
		// if the record doesn't have any relationships, none of its duplicates need to change
		if (before == null || before.getOutbounds().isEmpty()) {
			return false;
		}

		List<Vertex> duplicateRegistryObject = before.getIdentical().stream()
				.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.filter(vertex -> !vertex.getIdentifier().equals(before.getRegistryObjectId()))
				.collect(Collectors.toList());

		// if the record doesn't have any duplicate RegistryObject, this side effect does not apply
		if (duplicateRegistryObject.isEmpty() ) {
			return false;
		}

		// at this point, the record used to have outbound relationships and have previous duplicate RegistryObject
		// if the record is deleted, this side effect applies
		if (after == null) {
			return true;
		}

		// the record is modified, only concern is when it forgoes duplicates
		List<Vertex> forgoDuplicates = before.getIdentical().stream()
				.filter(vertex -> after.getIdentical().contains(vertex)).collect(Collectors.toList());

		return forgoDuplicates.size() > 0;
	}

	@Override
	public void handle() {
		List<String> priorDuplicateIDs = sideEffect.getPriorDuplicateIDs();
		MyceliumIndexingService myceliumIndexingService = getMyceliumService().getMyceliumIndexingService();

		// reindex all duplicates in an effort to remove invalidated relationships
		for (String registryObjectId : priorDuplicateIDs) {
			Vertex vertex = getMyceliumService().getVertexFromRegistryObjectId(registryObjectId);
			if (vertex != null) {
					myceliumIndexingService.indexVertex(vertex);

			} else {
				log.error("ForgoDuplicate. Vertex id=[{}] not found for re-indexing", registryObjectId);
			}
		}
	}

}
