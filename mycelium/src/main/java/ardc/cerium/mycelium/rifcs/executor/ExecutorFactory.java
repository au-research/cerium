package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.*;
import ardc.cerium.mycelium.service.MyceliumService;

public class ExecutorFactory {

	public static Executor get(SideEffect sideEffect, MyceliumService myceliumService) {
		if (sideEffect instanceof DuplicateInheritanceSideEffect) {
			return new DuplicateInheritanceExecutor((DuplicateInheritanceSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof TitleChangeSideEffect) {
			return new TitleChangeExecutor((TitleChangeSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof GrantsNetworkForgoSideEffect) {
			return new GrantsNetworkForgoExecutor((GrantsNetworkForgoSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof GrantsNetworkInheritenceSideEffect) {
			return new GrantsNetworkInheritenceExecutor((GrantsNetworkInheritenceSideEffect) sideEffect,
					myceliumService);
		}
		else if (sideEffect instanceof PrimaryKeyAdditionSideEffect) {
			return new PrimaryKeyAdditionExecutor((PrimaryKeyAdditionSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof PrimaryKeyDeletionSideEffect) {
			return new PrimaryKeyDeletionExecutor((PrimaryKeyDeletionSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof RelatedInfoRealisationSideEffect) {
			return new RelatedInfoRealisationExecutor((RelatedInfoRealisationSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof DuplicateForgoSideEffect) {
			return new DuplicateForgoExecutor((DuplicateForgoSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof IdentifierForgoSideEffect) {
			return new IdentifierForgoExecutor((IdentifierForgoSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof DirectRelationshipChangedSideEffect) {
			return new DirectRelationshipChangedExecutor((DirectRelationshipChangedSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof DCIRelationChangeSideEffect) {
			return new DCIRelationChangeExecutor((DCIRelationChangeSideEffect) sideEffect, myceliumService);
		}
		else if (sideEffect instanceof ScholixRelationChangeSideEffect) {
			return new ScholixRelationChangeExecutor((ScholixRelationChangeSideEffect) sideEffect, myceliumService);
		}
		return null;
	}

}
