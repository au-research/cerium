package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.service.*;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({ SpringExtension.class })
class DeleteTaskTest {

    @MockBean
    MyceliumService myceliumService;

    @MockBean
    MyceliumSideEffectService myceliumSideEffectService;

    @MockBean
    MyceliumRequestService myceliumRequestService;

    @MockBean
    RequestService requestService;

    @MockBean
    GraphService graphService;

    @Mock
    private Logger loggerMock;

    @BeforeEach
    void setUp() {
        when(myceliumService.getMyceliumSideEffectService()).thenReturn(myceliumSideEffectService);
        when(myceliumService.getGraphService()).thenReturn(graphService);
        when(myceliumService.getMyceliumRequestService()).thenReturn(myceliumRequestService);
        when(myceliumRequestService.getRequestService()).thenReturn(requestService);
        when(requestService.getLoggerFor(any(Request.class))).thenReturn(loggerMock);
    }

    @Test
    @DisplayName("A delete task with a record_id calls the deleteRecord method from myceliumService")
    void runTaskCallsIngest() throws Exception {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setAttribute(Attribute.RECORD_ID, "9999999");
        DeleteTask task = new DeleteTask(request, myceliumService);

        task.run();
        // it doesn't call delete if record doesn't exist
        Mockito.verify(myceliumService, times(1)).getVertexFromRegistryObjectId("9999999");
    }

}