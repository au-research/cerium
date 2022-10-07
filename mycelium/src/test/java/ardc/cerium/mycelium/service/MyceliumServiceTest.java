package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.event.DataSourceUpdatedEvent;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.repository.VertexRepository;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({MyceliumService.class})
class MyceliumServiceTest {

	@Autowired
	MyceliumService myceliumService;

	@MockBean
	GraphService graphService;

	@MockBean
	MyceliumRequestService myceliumRequestService;

	@MockBean
	MyceliumIndexingService myceliumIndexingService;

	@MockBean
	MyceliumSideEffectService myceliumSideEffectService;

	@Test
	void init() {
		// MyceliumSideEffectService#setMyceliumService(MyceliumService) is set upon init
		myceliumService.init();
		verify(myceliumSideEffectService, atLeastOnce()).setMyceliumService(myceliumService);
	}

	@Test
	void getVertexFromRegistryObjectId() {
		String id = UUID.randomUUID().toString();
		myceliumService.getVertexFromRegistryObjectId(id);
		verify(graphService, times(1)).getVertexByIdentifier(id, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
	}

	@Test
	void getRecordState() {
		String id = UUID.randomUUID().toString();
		myceliumService.getRecordState(id);
		verify(graphService, times(1)).getRecordState(id);
	}

	@Test
	void createRequest() {
		RequestDTO dto = new RequestDTO();
		myceliumService.createRequest(dto);
		verify(myceliumRequestService, times(1)).createRequest(dto);
	}

	@Test
	void saveToPayloadPath() {
		Request request = new Request();
		myceliumService.saveToPayloadPath(request, "abc");
		verify(myceliumRequestService, times(1)).saveToPayloadPath(request, "abc");
	}

	@Test
	void save() {
		Request request = new Request();
		myceliumService.save(request);
		verify(myceliumRequestService, times(1)).save(request);
	}

	@Test
	void findById() {
		String id = UUID.randomUUID().toString();
		myceliumService.findById(id);
		verify(myceliumRequestService, times(1)).findById(id);
	}

	@Test
	void validateRequest() {
		Request request = new Request();
		request.setType(MyceliumRequestService.IMPORT_REQUEST_TYPE);
		myceliumService.validateRequest(request);
		verify(myceliumRequestService, times(1)).validateImportRequest(request);
	}

	@Test
	void validateRequestNull() {
		Request request = new Request();
		request.setType("Unknown");
		myceliumService.validateRequest(request);
		verify(myceliumRequestService, times(0)).validateImportRequest(request);
	}

	@Test
	void indexVertex() {
		Vertex vertex = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		vertex.setStatus(Vertex.Status.PUBLISHED.name());
		myceliumService.indexVertex(vertex, false);
		verify(myceliumIndexingService, times(1)).indexVertex(vertex, false);
	}

	@Test
	void deleteRecord() throws Exception {
		Vertex vertex = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		when(myceliumService.getVertexFromRegistryObjectId(vertex.getIdentifier())).thenReturn(vertex);
		myceliumService.deleteRecord(vertex.getIdentifier());
		verify(graphService, times(1)).deleteVertex(vertex);
		verify(myceliumIndexingService, times(1)).deleteRelationship(vertex.getIdentifier());
	}

	@Test
	void parsePayloadToRegistryObject() throws JsonProcessingException {
		try (MockedStatic<RIFCSGraphProvider> util = Mockito.mockStatic(RIFCSGraphProvider.class)) {
			myceliumService.parsePayloadToRegistryObject("payload");
			util.verify(() -> RIFCSGraphProvider.parsePayloadToRegistryObject("payload"));
		}
	}

	@Test
	void getRegistryObjectVertexFromKey() {
		myceliumService.getRegistryObjectVertexFromKey("test");
		verify(graphService, times(1)).getRegistryObjectByKey("test");
	}

	@Test
	void getIdentifierVertex() {
		myceliumService.getIdentifierVertex("test", "doi");
		verify(graphService, times(1)).getVertexByIdentifier("test", "doi");
	}

	@Test
	void getDataSources() {
		myceliumService.getDataSources();
		verify(graphService, times(1)).getVertexIdentifiersByType(RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
	}

	@Test
	void deleteDataSourceById() {
		Vertex dataSourceVertex = new Vertex("1", RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
		when(graphService.getVertexByIdentifier("1", RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE)).thenReturn(dataSourceVertex);
		myceliumService.deleteDataSourceById("1");
		verify(graphService, times(1)).getVertexByIdentifier("1", RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
		verify(graphService, times(1)).deleteVertex(dataSourceVertex);
	}

	@Test
	void getVerticesByDataSource() {
		VertexRepository vertexRepository = Mockito.mock(VertexRepository.class);
		when(graphService.getVertexRepository()).thenReturn(vertexRepository);
		DataSource ds = new DataSource();
		ds.setId("1");
		myceliumService.getVerticesByDataSource(ds, Pageable.unpaged());
		verify(vertexRepository, times(1)).getVertexByDataSourceId("1", Pageable.unpaged());
	}

	@Test
	void deleteVerticesByDataSource() {
		VertexRepository vertexRepository = Mockito.mock(VertexRepository.class);
		when(graphService.getVertexRepository()).thenReturn(vertexRepository);
		DataSource ds = new DataSource();
		ds.setId("1");
		myceliumService.deleteVerticesByDataSource(ds);
		verify(vertexRepository, times(1)).deleteByDataSourceId("1");
	}

	@Test
	void publishEvent() {
		ApplicationEventPublisher applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
		ReflectionTestUtils.setField(myceliumService, "applicationEventPublisher", applicationEventPublisher);
		myceliumService.publishEvent(new DataSourceUpdatedEvent(myceliumService, "1", "log"));
		verify(applicationEventPublisher).publishEvent(any(ApplicationEvent.class));
	}
}