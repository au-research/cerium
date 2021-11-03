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

		return null;
	}

}
