package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.event.RegenerateMetadataEvent;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DCIRelationChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.util.RelationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Slf4j
public class DCIRelationChangeExecutor extends Executor{

    private final DCIRelationChangeSideEffect sideEffect;

    public DCIRelationChangeExecutor(DCIRelationChangeSideEffect sideEffect, MyceliumService myceliumService) {
        this.sideEffect = sideEffect;
        this.setMyceliumService(myceliumService);
    }

    public static boolean detect(RecordState before, RecordState after, MyceliumService myceliumService) {
        List<Relationship> differences = RelationUtil.getRelationshipsDifferences(before, after);
        return differences.stream().anyMatch(RelationUtil::isDCIRelation);
    }

    @Override
    public void handle() {
        RegenerateMetadataEvent event = new RegenerateMetadataEvent(this, sideEffect.getAffectedRegistryObjectId());
        event.setDci(true);
        getMyceliumService().publishEvent(event);
    }
}
