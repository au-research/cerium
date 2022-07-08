package ardc.cerium.mycelium.task;

import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class DeleteDataSourceTaskTest {

    @MockBean
    MyceliumService myceliumService;

    @Test
	void run() {
        DeleteDataSourceTask task = new DeleteDataSourceTask(myceliumService, "1");
        task.run();
        verify(myceliumService, times(1)).deleteDataSourceById("1");
	}
}