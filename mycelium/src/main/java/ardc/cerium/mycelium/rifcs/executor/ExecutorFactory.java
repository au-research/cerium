package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.rifcs.effect.GrantsNetworkForgoSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
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

		return null;
	}

}
