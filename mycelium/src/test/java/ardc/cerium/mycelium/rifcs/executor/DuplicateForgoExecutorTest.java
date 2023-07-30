package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateForgoSideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DuplicateForgoExecutorTest {

    @MockBean
    MyceliumService myceliumService;

	@Test
	void detect() {
        RecordState before = new RecordState();
        before.setRegistryObjectId("1");
        Vertex id1 = new Vertex("2", "ro:id");
        id1.addLabel(Vertex.Label.RegistryObject);
        Vertex id2 = new Vertex("3", "ro:id");
        id2.addLabel(Vertex.Label.RegistryObject);
        before.setIdentical(Arrays.asList(id1, id2));
        before.setOutbounds(Arrays.asList(new Relationship()));

        RecordState after = new RecordState();
        after.setIdentical(Arrays.asList(id1));

        assertThat(DuplicateForgoExecutor.detect(before, after, myceliumService)).isTrue();
	}

    @Test
	void handle() {
        DuplicateForgoSideEffect sideEffect = new DuplicateForgoSideEffect(Arrays.asList("1"));
        MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
        when(myceliumService.getMyceliumIndexingService()).thenReturn(indexingService);
        when(myceliumService.getVertexFromRegistryObjectId(any())).thenReturn(new Vertex("1", "ro:id"));
        DuplicateForgoExecutor executor = new DuplicateForgoExecutor(sideEffect, myceliumService);
        executor.handle();
        verify(indexingService, times(1)).indexVertex(any());
	}
}