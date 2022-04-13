package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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



}