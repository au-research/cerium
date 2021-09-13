package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({ SpringExtension.class })
class ImportTaskTest {

	@MockBean
	MyceliumService myceliumService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@BeforeEach
	void setUp() {
		when(myceliumService.getMyceliumSideEffectService()).thenReturn(myceliumSideEffectService);
	}

	@Test
	@DisplayName("Given a task with a payload, when the ImportTask is ran, it calls the ingest method from myceliumService")
	void runTaskCallsIngest() throws IOException {
		Request request = new Request();
		request.setId(UUID.randomUUID());
		request.setAttribute(Attribute.PAYLOAD_PATH,
				"src/test/resources/scenarios/1_RelationshipScenario/1_RelationshipScenario.xml");
		ImportTask task = new ImportTask(request, myceliumService);

		RegistryObject mockedRegistryObject = new RegistryObject();
		mockedRegistryObject.setRegistryObjectId(1L);

		when(myceliumSideEffectService.detectChanges(any(RecordState.class), any(RecordState.class)))
				.thenReturn(new ArrayList<>());
		when(myceliumService.parsePayloadToRegistryObject(any(String.class))).thenReturn(mockedRegistryObject);
		when(myceliumService.getRecordState(mockedRegistryObject.getRegistryObjectId().toString()))
				.thenReturn(new RecordState());
		task.run();

		verify(myceliumService, times(1)).parsePayloadToRegistryObject(any(String.class));
		verify(myceliumService, times(2)).getRecordState(mockedRegistryObject.getRegistryObjectId().toString());
		verify(myceliumService, times(1)).ingestRegistryObject(any(RegistryObject.class));
		verify(myceliumSideEffectService, times(1)).detectChanges(any(RecordState.class), any(RecordState.class));
		verify(myceliumSideEffectService, times(1)).queueSideEffects(any(Request.class), anyList());
	}

}