package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({ SpringExtension.class })
class ImportTaskTest {

	@Mock
	MyceliumService myceliumService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@Test
	@DisplayName("Given a task with a payload, when the import task is run, it calls the ingest method from myceliumService")
	void runTaskCallsIngest() throws IOException {
		Request request = new Request();
		request.setId(UUID.randomUUID());
		request.setAttribute(Attribute.PAYLOAD_PATH,
				"src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
		String rifcs = Helpers.readFile("src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
		ImportTask task = new ImportTask(request, myceliumService, myceliumSideEffectService);

		RegistryObject mockedRegistryObject = new RegistryObject();
		mockedRegistryObject.setRegistryObjectId(1L);
		when(myceliumService.parsePayloadToRegistryObject(any(String.class))).thenReturn(mockedRegistryObject);

		task.run();
		Mockito.verify(myceliumService, times(1)).ingestRegistryObject(any(RegistryObject.class));
	}

}