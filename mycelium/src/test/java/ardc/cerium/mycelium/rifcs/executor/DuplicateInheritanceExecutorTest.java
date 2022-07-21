package ardc.cerium.mycelium.rifcs.executor;

import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.EdgeDTO;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.DuplicateInheritanceSideEffect;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DuplicateInheritanceExecutorTest {

	@MockBean
	MyceliumService myceliumService;


	@Test
	void detect() {
        // given the after state has duplicates and no before state
        RecordState after = new RecordState();
		after.setRegistryObjectId("1");
		after.setIdentifiers(Arrays.asList(new Vertex("id", "doi")));

        // when detect, then it's true
		GraphService graphService = Mockito.mock(GraphService.class);
		when(myceliumService.getVertexFromRegistryObjectId(any())).thenReturn(new Vertex());
		when(graphService.getSameAsRegistryObject(any())).thenReturn(Arrays.asList(new Vertex("id", "orcid")));
		when(graphService.getDirectOutboundRelationships(any(), any())).thenReturn(Arrays.asList(new Relationship()));
		when(myceliumService.getGraphService()).thenReturn(graphService);

		assertThat(DuplicateInheritanceExecutor.detect(null, after, myceliumService)).isTrue();
	}

	@Test
	void handle() {
		DuplicateInheritanceSideEffect sideEffect = new DuplicateInheritanceSideEffect("1");
		GraphService graphService = Mockito.mock(GraphService.class);
		MyceliumIndexingService indexingService = Mockito.mock(MyceliumIndexingService.class);
		when(graphService.getVertexByIdentifier("1", RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE))
				.thenReturn(new Vertex());
		when(graphService.getSameAsRegistryObject(any())).thenReturn(Arrays.asList(new Vertex("id", "orcid")));

		Relationship outbound = new Relationship();
		outbound.setTo(new Vertex("to", "ro:id"));
		EdgeDTO edgeDTO = new EdgeDTO();
		edgeDTO.setType("hasAssociationWith");
		outbound.setRelations(Arrays.asList(edgeDTO));
		when(graphService.getDirectOutboundRelationships(any(), any())).thenReturn(Arrays.asList(outbound));

		when(myceliumService.getGraphService()).thenReturn(graphService);
		when(myceliumService.getIndexingService()).thenReturn(indexingService);

		DuplicateInheritanceExecutor executor = new DuplicateInheritanceExecutor(sideEffect, myceliumService);
		executor.handle();
		verify(indexingService, times(2)).indexRelation(any(), any(), any());
	}
}