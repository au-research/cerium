package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyAdditionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({ SpringExtension.class })
class ImportDataSourceTaskTest {

	@MockBean
	MyceliumService myceliumService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@MockBean
	GraphService graphService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@MockBean
	RequestService requestService;

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
	void run_noSideEffects() {
		DataSource ds = new DataSource();
		ds.setId("1");

		when(myceliumService.getDataSourceById("1")).thenReturn(ds);
		when(myceliumSideEffectService.detectChanges(any(DataSource.class), any(DataSource.class)))
				.thenReturn(new ArrayList<>());

		ImportDataSourceTask task = new ImportDataSourceTask(myceliumService, ds);
		task.run();

		verify(myceliumService, times(1)).deleteDataSourceById("1");
		verify(myceliumService, times(1)).importDataSource(ds);
	}

	@Test
	void run_withSideEffects() {
		DataSource ds = new DataSource();
		ds.setId("1");

		when(myceliumService.getDataSourceById("1")).thenReturn(ds);
		when(myceliumSideEffectService.detectChanges(any(DataSource.class), any(DataSource.class)))
				.thenReturn(Arrays.asList(new PrimaryKeyAdditionSideEffect("1", new PrimaryKey())));
		Request request = new Request();
		request.setId(UUID.randomUUID());
		when(myceliumService.createRequest(any())).thenReturn(request);
		when(myceliumSideEffectService.getQueueID(any())).thenReturn("test-queue-id");

		ImportDataSourceTask task = new ImportDataSourceTask(myceliumService, ds);
		task.run();

		verify(myceliumService, times(1)).deleteDataSourceById("1");
		verify(myceliumService, times(1)).importDataSource(ds);
		verify(myceliumSideEffectService, times(1)).addToQueue(any(), any());
	}
}