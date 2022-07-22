package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.IdentifierForgoSideEffect;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.solr.core.query.result.Cursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class IdentifierForgoExecutorTest {


    @Test
    void detect_Identifiers_NotChanged() {
        RecordState before = new RecordState();
        RecordState after = new RecordState();
        Collection<Vertex> oldIdentifiers = new ArrayList<>();
        Collection<Vertex> newIdentifiers = new ArrayList<>();
        Vertex v1 = new Vertex("10.222", "doi");
        v1.setStatus("PUBLISHED");
        Vertex v2 = new Vertex("1378.2/1234", "handle");
        v2.setStatus("PUBLISHED");
        newIdentifiers.add(v1);
        newIdentifiers.add(v2);
        oldIdentifiers.add(v1);
        oldIdentifiers.add(v2);
        before.setIdentifiers(oldIdentifiers);
        before.setStatus("PUBLISHED");
        after.setIdentifiers(newIdentifiers);
        after.setStatus("PUBLISHED");

        assertThat(IdentifierForgoExecutor.detect(before, after)).isFalse();
    }

    @Test
    void detect_Identifiers_lost() {
        RecordState before = new RecordState();
        RecordState after = new RecordState();
        Collection<Vertex> oldIdentifiers = new ArrayList<>();
        Collection<Vertex> newIdentifiers = new ArrayList<>();
        Vertex v1 = new Vertex("10.222", "doi");
        Vertex v2 = new Vertex("1378.2/1234", "handle");
        v1.setStatus("PUBLISHED");
        v2.setStatus("PUBLISHED");
        oldIdentifiers.add(v1);
        oldIdentifiers.add(v2);
        // only add one Vertex to the new state
        newIdentifiers.add(v1);

        before.setIdentifiers(oldIdentifiers);
        after.setIdentifiers(newIdentifiers);
        before.setStatus("PUBLISHED");
        after.setStatus("PUBLISHED");
        assertThat(IdentifierForgoExecutor.detect(before, after)).isTrue();
    }

    @Test
	void detect_created_or_no_identifiers() {
        RecordState after = new RecordState();
        assertThat(IdentifierForgoExecutor.detect(null, after)).isFalse();

        RecordState before = new RecordState();
        assertThat(IdentifierForgoExecutor.detect(before, after)).isFalse();
	}

    @Test
	void deleted_or_draft() {
        RecordState before = new RecordState();
        before.setIdentifiers(Arrays.asList(new Vertex("1", "doi")));
        assertThat(IdentifierForgoExecutor.detect(before, null)).isFalse();

        RecordState after = new RecordState();
        after.setStatus(Vertex.Status.DRAFT.name());
        assertThat(IdentifierForgoExecutor.detect(before, after)).isFalse();

        after.setStatus(Vertex.Status.PUBLISHED.name());
        assertThat(IdentifierForgoExecutor.detect(before, after)).isFalse();
	}

    @Test
	void handle() {
        IdentifierForgoSideEffect sideEffect = new IdentifierForgoSideEffect("1", "deleted-identifier",
				"deleted-identifier-type", "searchTitle", "recordClass", "recordType");

        MyceliumService myceliumService = Mockito.mock(MyceliumService.class);
        MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
        GraphService graphService = Mockito.mock(GraphService.class);
        when(myceliumService.getIndexingService()).thenReturn(indexingService);
        when(myceliumService.getGraphService()).thenReturn(graphService);
        when(indexingService.cursorFor(any())).thenReturn(Mockito.mock(Cursor.class));

        IdentifierForgoExecutor executor = new IdentifierForgoExecutor(sideEffect, myceliumService);
        executor.handle();
        verify(indexingService, times(1)).cursorFor(any());
        verify(myceliumService, times(1)).getIdentifierVertex(any(), any());
        verify(graphService, times(1)).getDirectInboundRelationships(any(), any());
	}
}