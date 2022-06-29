package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.event.RegenerateMetadataEvent;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.ScholixRelationChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;

import java.util.List;

public class ScholixRelationChangeExecutor extends Executor{

    private final ScholixRelationChangeSideEffect sideEffect;

    public ScholixRelationChangeExecutor(ScholixRelationChangeSideEffect sideEffect, MyceliumService myceliumService) {
        this.sideEffect = sideEffect;
        this.setMyceliumService(myceliumService);
    }

    public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
        List<Relationship> differences = RelationUtil.getRelationshipsDifferences(before, after);
        return differences.stream().anyMatch(RelationUtil::isScholixRelation);
    }

    @Override
    public void handle() {
        RegenerateMetadataEvent event = new RegenerateMetadataEvent(this, sideEffect.getAffectedRegistryObjectId());
        event.setScholix(true);
        getMyceliumService().publishEvent(event);
    }
}
