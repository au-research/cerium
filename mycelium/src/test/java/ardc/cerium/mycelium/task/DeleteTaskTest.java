package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.times;

@ExtendWith({ SpringExtension.class })
class DeleteTaskTest {

    @Mock
    MyceliumService myceliumService;

    @Test
    @DisplayName("A delete task with a record_id calls the deleteRecord method from myceliumService")
    void runTaskCallsIngest() throws IOException {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setAttribute(Attribute.RECORD_ID, "9999999");
        DeleteTask task = new DeleteTask(request, myceliumService);

        task.run();
        Mockito.verify(myceliumService, times(1)).deleteRecord("9999999", request);
    }

}