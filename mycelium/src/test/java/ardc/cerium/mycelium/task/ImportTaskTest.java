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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith({ SpringExtension.class })
class ImportTaskTest {

	@Mock
	MyceliumService myceliumService;

	@Test
	@DisplayName("Given a task with a payload, when the import task is run, it calls the ingest method from myceliumService")
	void runTaskCallsIngest() {
		Request request = new Request();
		request.setId(UUID.randomUUID());
		request.setAttribute(Attribute.PAYLOAD_PATH,
				"src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
		ImportTask task = new ImportTask(request, myceliumService);

		task.run();
		Mockito.verify(myceliumService, times(1)).ingest(any(String.class));
	}

}