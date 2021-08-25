package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;

public class ExecutorFactory {

    public static Executor get(SideEffect sideEffect, GraphService graphService, MyceliumIndexingService myceliumIndexingService) {
        if (sideEffect instanceof DuplicateInheritanceSideEffect) {
            return new DuplicateInheritanceExecutor((DuplicateInheritanceSideEffect) sideEffect, graphService, myceliumIndexingService);
        } else if (sideEffect instanceof TitleChangeSideEffect) {
            return new TitleChangeExecutor((TitleChangeSideEffect) sideEffect, myceliumIndexingService);
        }

        return null;
    }
}
