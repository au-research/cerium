package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TitleChangeExecutor extends Executor {

	private final TitleChangeSideEffect sideEffect;

	private final MyceliumIndexingService myceliumIndexingService;

	public TitleChangeExecutor(TitleChangeSideEffect sideEffect, MyceliumIndexingService myceliumIndexingService) {
		this.sideEffect = sideEffect;
		this.myceliumIndexingService = myceliumIndexingService;
	}

	@Override
	public void handle() {
		log.debug("Handling SideEffect {}", sideEffect);
		myceliumIndexingService.updateTitle(sideEffect.getRegistryObjectId(), sideEffect.getNewTitle());
		// todo update in portal collection
	}

}
