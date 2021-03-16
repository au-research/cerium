package ardc.cerium.drvs.service;

import ardc.cerium.drvs.TestHelper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.repository.RequestRepository;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.RequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DRVSImportService.class)
@TestPropertySource(properties = "app.drvs.enabled=true")
class DRVSImportServiceTest {

	@Autowired
    DRVSImportService drvsImportService;

	@MockBean
	RequestService requestService;

	@MockBean
	RequestRepository requestRepository;

	@MockBean
	RecordService recordService;

	@MockBean
	DRVSVersionService drvsVersionService;

	@MockBean
	IdentifierService identifierService;

	@Test
	void save_will_call_repository_save() {
		// given a request
		Request request = TestHelper.mockRequest();
		drvsImportService.save(request);
		verify(requestService, times(1)).save(any(Request.class));
	}

	// TODO createRequest

	// TODO ingest

}