package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RelatedInfoRealisationExecutorTest {

	@MockBean
	MyceliumService myceliumService;

	@MockBean
	GraphService graphService;

	@Test
	void detect_deletedRegistryObject() {
		RecordState before = new RecordState();

		assertThat(RelatedInfoRealisationExecutor.detect(before, null, myceliumService)).isFalse();
	}

	@Test
	void detect_newIdentifiers() {
		RecordState before = new RecordState();

		RecordState after = new RecordState();
		Collection<Vertex> identical = new HashSet<>();
		identical.add(new Vertex("102.7/3", "doi"));
		after.setIdentical(identical);
		before.setStatus("PUBLISHED");
		after.setStatus("PUBLISHED");
		when(myceliumService.getGraphService()).thenReturn(graphService);
		Collection<Relationship> mockedResponse = new HashSet<>();
		mockedResponse.add(new Relationship());
		when(graphService.getDirectOutboundRelationships("102.7/3", "doi")).thenReturn(mockedResponse);

		assertThat(RelatedInfoRealisationExecutor.detect(before, after, myceliumService)).isTrue();
	}

	@Test
	void detect_noChange() {
		Collection<Vertex> identicalBefore = new HashSet<>();
		identicalBefore.add(new Vertex("1", "ro:id"));
		RecordState before = new RecordState();
		before.setIdentical(identicalBefore);

		Collection<Vertex> identicalAfter = new HashSet<>();
		identicalAfter.add(new Vertex("1", "ro:id"));
		RecordState after = new RecordState();
		after.setIdentical(identicalAfter);
		before.setStatus("PUBLISHED");
		after.setStatus("PUBLISHED");
		when(myceliumService.getGraphService()).thenReturn(graphService);
		Collection<Relationship> mockedResponse = new HashSet<>();
		mockedResponse.add(new Relationship());
		when(graphService.getDirectOutboundRelationships("1", "ro:id")).thenReturn(mockedResponse);

		assertThat(RelatedInfoRealisationExecutor.detect(before, after, myceliumService)).isFalse();
		assertThat(RelatedInfoRealisationExecutor.detect(after, before, myceliumService)).isFalse();
	}

}