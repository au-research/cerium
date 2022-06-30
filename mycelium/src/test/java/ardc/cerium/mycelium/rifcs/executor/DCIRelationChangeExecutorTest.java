package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.event.RegenerateMetadataEvent;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DCIRelationChangeSideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import jdk.jfr.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({ SpringExtension.class })
class DCIRelationChangeExecutorTest {

    @MockBean
    MyceliumService myceliumService;

    @Test
	void detect_positive() {
        // before: p1
        // after: p1 isOwnerOf c1

        Vertex p1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        p1.setObjectClass("party");
        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");

        Relationship afterRelation = new Relationship();
        afterRelation.setFrom(p1);
        afterRelation.setTo(c1);
        EdgeDTO P1IsOwnerOfC1 = new EdgeDTO();
        P1IsOwnerOfC1.setType("isOwnerOf");
        afterRelation.setRelations(List.of(P1IsOwnerOfC1));

        RecordState before = new RecordState();
        RecordState after = new RecordState();
        after.setOutbounds(Collections.singleton(afterRelation));

        assertThat(DCIRelationChangeExecutor.detect(before, after, myceliumService)).isTrue();
	}

    @Test
    void detect_nochange() {
        assertThat(DCIRelationChangeExecutor.detect(new RecordState(), new RecordState(), myceliumService)).isFalse();
    }

    @Test
    void detect_not_relevant() {
        Vertex p1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        p1.setObjectClass("party");
        Vertex c1 = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
        c1.setObjectClass("collection");

        Relationship afterRelation = new Relationship();
        afterRelation.setFrom(p1);
        afterRelation.setTo(c1);
        EdgeDTO P1HasAssociationWithC1 = new EdgeDTO();
        P1HasAssociationWithC1.setType("hasAssociationWith");
        afterRelation.setRelations(List.of(P1HasAssociationWithC1));

        RecordState before = new RecordState();
        RecordState after = new RecordState();
        after.setOutbounds(Collections.singleton(afterRelation));

        assertThat(DCIRelationChangeExecutor.detect(before, after, myceliumService)).isFalse();
    }

    @Test
	void handle_calls_event() {

        DCIRelationChangeSideEffect sideEffect = new DCIRelationChangeSideEffect("1");
		DCIRelationChangeExecutor executor = new DCIRelationChangeExecutor(sideEffect, myceliumService);
        executor.handle();

        Mockito.verify(myceliumService, times(1)).publishEvent(any(RegenerateMetadataEvent.class));
	}
}