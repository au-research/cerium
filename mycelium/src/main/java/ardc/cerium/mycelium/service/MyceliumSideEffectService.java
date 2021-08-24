package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumSideEffectService {

	private final GraphService graphService;

	private final MyceliumIndexingService indexingService;

	public MyceliumSideEffectService(GraphService graphService, MyceliumIndexingService indexingService) {
		this.graphService = graphService;
		this.indexingService = indexingService;
	}

	/**
	 * Detect changes
	 *
	 * TODO add Request tracking
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public List<SideEffect> detectChanges(RecordState before, RecordState after) {
		List<SideEffect> sideEffects = new ArrayList<>();

		// this shouldn't happen
		if (before == null && after == null) {
			return sideEffects;
		}

		if (detectDuplicateInheritanceSideEffect(before, after)) {
			sideEffects.add(
					new DuplicateInheritanceSideEffect(after.getRegistryObjectId(), graphService, indexingService));
		}

		if (before != null && detectTitleChange(before, after)) {
			sideEffects
					.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
		}

		return sideEffects;
	}

	/**
	 * Detect if {@link DuplicateInheritanceSideEffect} is applicable
	 *
	 * A record is considered to have duplicate inheritance is when it is created, and
	 * brings with it inheritable relationships to its duplicates
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public boolean detectDuplicateInheritanceSideEffect(RecordState before, RecordState after) {

		// only applies for record creation
		// todo investigate if this is necessary/applicable?
		if (before != null) {
			return false;
		}

		// if the afterStates have duplicate registryObject
		Vertex afterVertex = graphService.getVertexByIdentifier(after.getRegistryObjectId(),
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Collection<Vertex> myDuplicates = graphService.getSameAsRegistryObject(afterVertex).stream()
				.filter(v -> !v.getIdentifier().equals(afterVertex.getIdentifier())).collect(Collectors.toList());
		if (myDuplicates.size() == 0) {
			// only applicable if the record has duplicates
			return false;
		}

		Collection<Relationship> directOutbounds = graphService
				.getDirectOutboundRelationships(afterVertex.getIdentifier(), afterVertex.getIdentifierType());
		// only applicable if the record has direct outbound links
		return directOutbounds.size() != 0;
	}

	/**
	 * Detect if {@link TitleChangeSideEffect} is applicable
	 *
	 * This {@link SideEffect} is applicable when the after {@link RecordState} has a
	 * different title than the before state
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @return boolean
	 */
	public boolean detectTitleChange(RecordState before, RecordState after) {
		return !before.getTitle().equals(after.getTitle());
	}

	/**
	 * Handle all provided {@link SideEffect}
	 * @param sideEffects a {@link List} of {@link SideEffect}
	 */
	public void handleSideEffects(List<SideEffect> sideEffects) {
		sideEffects.forEach(SideEffect::handle);
	}

}
