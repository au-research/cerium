package ardc.cerium.mycelium.service;

import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RIFCSParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MyceliumService.class })
class MyceliumServiceTest {

	@Autowired
	MyceliumService myceliumService;

	@MockBean
	GraphService graphService;

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
}