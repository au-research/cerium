package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.PrimaryKeyDeletionSideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.PrimaryKeySetting;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.service.GraphService;
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
class PrimaryKeyDeletionExecutorTest {

	@MockBean
	MyceliumService myceliumService;

	@Test
	void detect() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));

		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();

		assertThat(PrimaryKeyDeletionExecutor.detect(before, after, myceliumService)).isTrue();
	}

	@Test
	void detect_noChange() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));

		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk2 = new PrimaryKey();
		pk2.setKey("ARDC");
		pk2.setRelationTypeFromCollection("isFundedBy");
		afterPKSetting.getPrimaryKeys().add(pk2);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyDeletionExecutor.detect(after, before, myceliumService)).isFalse();
	}

	@Test
	void detect_relationTypeChange() {
		when(myceliumService.getRegistryObjectVertexFromKey("ARDC")).thenReturn(new Vertex("1", "ro:id"));
		DataSource before = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		before.setPrimaryKeySetting(beforePKSetting);

		DataSource after = new DataSource();
		PrimaryKeySetting afterPKSetting = new PrimaryKeySetting();
		PrimaryKey pk2 = new PrimaryKey();
		pk2.setKey("ARDC");
		pk2.setRelationTypeFromCollection("hasAssociationWith");
		afterPKSetting.getPrimaryKeys().add(pk2);
		after.setPrimaryKeySetting(afterPKSetting);

		assertThat(PrimaryKeyDeletionExecutor.detect(before, after, myceliumService)).isTrue();
	}

	@Test
	void detect_recordRemovedbeingPrimaryKey_notDeleted() {

		assertThat(PrimaryKeyDeletionExecutor.detect(new RecordState(), null, myceliumService)).isFalse();

		// given a record being created, and is not deleted, then it's not detected
		assertThat(PrimaryKeyDeletionExecutor.detect(null, new RecordState(), myceliumService)).isFalse();
	}

	@Test
	void detect_recordRemovedBeingPrimaryKey() {
		DataSource dataSource = new DataSource();
		PrimaryKeySetting beforePKSetting = new PrimaryKeySetting();
		PrimaryKey pk1 = new PrimaryKey();
		pk1.setKey("ARDC");
		pk1.setRelationTypeFromCollection("isFundedBy");
		beforePKSetting.getPrimaryKeys().add(pk1);
		dataSource.setPrimaryKeySetting(beforePKSetting);

		RecordState recordState = new RecordState();
		recordState.setRegistryObjectKey("ARDC");

		when(myceliumService.getDataSourceById(any())).thenReturn(dataSource);

		assertThat(PrimaryKeyDeletionExecutor.detect(recordState, null, myceliumService)).isTrue();
	}

	@Test
	void handle() {
		PrimaryKeyDeletionSideEffect sideEffect = new PrimaryKeyDeletionSideEffect("1", "key", "1", "party", "isFundedBy");
		when(myceliumService.getRegistryObjectVertexFromKey("key")).thenReturn(new Vertex());
		GraphService graphService = Mockito.mock(GraphService.class);
		when(graphService.streamRegistryObjectFromDataSource(any(), any())).thenAnswer(invocationOnMock -> Arrays.asList(new Vertex()).stream());
		MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
		when(myceliumService.getGraphService()).thenReturn(graphService);
		when(myceliumService.getMyceliumIndexingService()).thenReturn(indexingService);

		PrimaryKeyDeletionExecutor executor = new PrimaryKeyDeletionExecutor(sideEffect, myceliumService);

		executor.handle();
		verify(myceliumService, times(1)).getRegistryObjectVertexFromKey("key");
		verify(graphService, times(1)).deletePrimaryKeyEdge("key");
		verify(indexingService, times(1)).deletePrimaryKeyEdges("1");
		verify(graphService, times(2)).streamRegistryObjectFromDataSource(any(), any());
		verify(indexingService, times(2)).regenGrantsNetworkRelationships(any(Vertex.class));
	}
}