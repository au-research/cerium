package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MyceliumService.class })
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
	void indexVertex() {
		Vertex vertex = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		vertex.setStatus(Vertex.Status.PUBLISHED.name());
		myceliumService.indexVertex(vertex);
		verify(myceliumIndexingService, times(1)).indexVertex(vertex);
	}

	@Test
	void deleteRecord() throws Exception {
		Vertex vertex = new Vertex(UUID.randomUUID().toString(), RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		when(myceliumService.getVertexFromRegistryObjectId(vertex.getIdentifier())).thenReturn(vertex);
		myceliumService.deleteRecord(vertex.getIdentifier());
		verify(graphService, times(1)).deleteVertex(vertex);
		verify(myceliumIndexingService, times(1)).deleteRelationship(vertex.getIdentifier());
	}

}