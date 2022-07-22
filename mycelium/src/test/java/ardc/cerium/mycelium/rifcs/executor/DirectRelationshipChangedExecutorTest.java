package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DirectRelationshipChangedSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DirectRelationshipChangedExecutorTest {

    @MockBean
    MyceliumService myceliumService;

	@Test
	void detect_addition() {
        Relationship outbound = new Relationship();
        outbound.setTo(new Vertex());

        RecordState before = new RecordState();

        RecordState after = new RecordState();
        after.setStatus(Vertex.Status.PUBLISHED.name());
        after.setOutbounds(Arrays.asList(outbound));

        assertThat(DirectRelationshipChangedExecutor.detect(before, after, myceliumService)).isTrue();
	}

    @Test
	void detect_removal() {
        Relationship outbound = new Relationship();
        outbound.setTo(new Vertex());

        RecordState before = new RecordState();
        before.setOutbounds(Arrays.asList(outbound));

        RecordState after = new RecordState();
        after.setStatus(Vertex.Status.PUBLISHED.name());

        assertThat(DirectRelationshipChangedExecutor.detect(before, after, myceliumService)).isTrue();
	}

    @Test
	void detect_draft() {
        RecordState before = new RecordState();

        RecordState after = new RecordState();
        after.setStatus(Vertex.Status.DRAFT.name());

        assertThat(DirectRelationshipChangedExecutor.detect(before, after, myceliumService)).isFalse();
	}

    @Test
	void detect_delete() {
        RecordState before = new RecordState();

        assertThat(DirectRelationshipChangedExecutor.detect(before, null, myceliumService)).isFalse();
	}

    @Test
	void handle_add() {
        DirectRelationshipChangedSideEffect sideEffect = new DirectRelationshipChangedSideEffect("1", "2", "add", "collection", "dataset", "title", "isPartOf");

        MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
        when(myceliumService.getMyceliumIndexingService()).thenReturn(indexingService);

        DirectRelationshipChangedExecutor executor = new DirectRelationshipChangedExecutor(sideEffect, myceliumService);
        executor.handle();
        verify(indexingService, times(1)).addRelatedTitleToPortalIndex("2", "collection", "dataset", "title", "isPartOf");
	}

    @Test
	void handle_delete() {
        DirectRelationshipChangedSideEffect sideEffect = new DirectRelationshipChangedSideEffect("1", "2", "delete", "collection", "dataset", "title", "isPartOf");

        MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
        when(myceliumService.getMyceliumIndexingService()).thenReturn(indexingService);

        DirectRelationshipChangedExecutor executor = new DirectRelationshipChangedExecutor(sideEffect, myceliumService);
        executor.handle();
        verify(indexingService, times(1)).deleteRelatedTitleFromPortalIndex("2", "collection", "dataset", "title", "isPartOf");
	}
}