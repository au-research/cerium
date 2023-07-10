package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class TitleChangeExecutor extends Executor {

	private final TitleChangeSideEffect sideEffect;

	private final MyceliumIndexingService myceliumIndexingService;

	public TitleChangeExecutor(TitleChangeSideEffect sideEffect, MyceliumService myceliumService) {
		this.sideEffect = sideEffect;
		this.setMyceliumService(myceliumService);
		this.myceliumIndexingService = myceliumService.getIndexingService();
	}

	/**
	 * Detect if {@link TitleChangeSideEffect} is applicable
	 *
	 * This {@link SideEffect} is applicable when the after {@link RecordState} has a
	 * different title than the before state
	 * @param before the before {@link RecordState}
	 * @param after the after {@link RecordState}
	 * @param myceliumService the {@link MyceliumService} for additional business logic
	 * @return boolean
	 */
	public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
		// if it's a DRAFT
		if ( after != null && after.getStatus().equals(Vertex.Status.DRAFT.name())) {
			return false;
		}
		return after != null && before != null && !before.getTitle().equals(after.getTitle());
	}

	@Override
	public void handle() {
		log.debug("Handling SideEffect {}", sideEffect);
		myceliumIndexingService.updateTitle(sideEffect.getAffectedRegistryObjectId(), sideEffect.getNewTitle());
		if(!sideEffect.getObjectClass().equals("collection")) {
			myceliumIndexingService.updateRelatedTitlesInPortalIndex(sideEffect.getObjectClass(), sideEffect.getObjectType(),
					sideEffect.getOldTitle(), sideEffect.getNewTitle(), sideEffect.getRelationshipType());
		}
	}

}
