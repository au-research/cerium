package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumSideEffectService {

    @Autowired
    GraphService graphService;

    /**
     * Detect changes
     *
     * TODO move to MyceliumSideEffectService
     * @param before
     * @param after
     * @return
     */
    public List<SideEffect> detectChanges(RecordState before, RecordState after) {
        List<SideEffect> sideEffects = new ArrayList<>();

        // this shouldn't happen
        if (before == null && after == null) {
            return sideEffects;
        }

        // record is created
        if (before == null) {
            // recordCreatedSideEffect
            // investigate after state grants network

            // if the afterStates have duplicate registryObject
            Vertex afterVertex = graphService.getVertexByIdentifier(after.getRegistryObjectId(),
                    RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
            Collection<Vertex> myDuplicates = graphService.getSameAsRegistryObject(afterVertex).stream()
                    .filter(v -> !v.getIdentifier().equals(afterVertex.getIdentifier())).collect(Collectors.toList());

            return sideEffects;
        }

        // record is deleted
        if (after == null) {
            // recordDeletedSideEffect
            // investigate before state grants network
            return sideEffects;
        }

        // detect title change
        if (! before.getTitle().equals(after.getTitle())) {
            sideEffects.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
        }

        // todo other change detection

        return sideEffects;

    }

    public void handleSideEffects(List<SideEffect> sideEffects) {
        sideEffects.forEach(SideEffect::handle);
    }
}
