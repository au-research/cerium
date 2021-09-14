package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({ SpringExtension.class })
class DeleteTaskTest {

    @MockBean
    MyceliumService myceliumService;

    @MockBean
    MyceliumSideEffectService myceliumSideEffectService;

    @MockBean
    GraphService graphService;

    @BeforeEach
    void setUp() {
        when(myceliumService.getMyceliumSideEffectService()).thenReturn(myceliumSideEffectService);
        when(myceliumService.getGraphService()).thenReturn(graphService);
    }

    @Test
    @DisplayName("A delete task with a record_id calls the deleteRecord method from myceliumService")
    void runTaskCallsIngest() throws Exception {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setAttribute(Attribute.RECORD_ID, "9999999");
        DeleteTask task = new DeleteTask(request, myceliumService);

        task.run();
        Mockito.verify(myceliumService, times(1)).deleteRecord("9999999");
    }

}