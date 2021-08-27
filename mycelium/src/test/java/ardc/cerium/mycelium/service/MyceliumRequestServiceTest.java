package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.exception.ContentNotSupportedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@Import({ MyceliumRequestService.class })
public class MyceliumRequestServiceTest {

	@Autowired
	MyceliumRequestService myceliumRequestService;

	@MockBean
	RequestService requestService;

	@MockBean
	RequestMapper requestMapper;

	@Test
	void validateRequestDTO_null() {
		assertThrows(ContentNotSupportedException.class,
				() -> myceliumRequestService.validateRequestDTO(new RequestDTO()));

	}

	@Test
	void validateRequestDTO_notValidType() {
		assertThrows(ContentNotSupportedException.class, () -> {
			RequestDTO dto = new RequestDTO();
			dto.setType("fish");
			myceliumRequestService.validateRequestDTO(dto);
		});
	}

	@Test
	void validateRequestDTO() {
		RequestDTO dto = new RequestDTO();
		dto.setType(MyceliumRequestService.AFFECTED_REL_REQUEST_TYPE);
		myceliumRequestService.validateRequestDTO(dto);
	}

	@Test
	void validatePayloadPathNotSet() {
		assertThrows(ContentNotSupportedException.class,
				() -> myceliumRequestService.validateImportRequest(new Request()));
	}

	@Test
	void validatePayloadPathNotAccessible() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "blah");
			myceliumRequestService.validateImportRequest(request);
		});
	}

	@Test
	void validatePayloadPathEmpty() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/empty.xml");
			myceliumRequestService.validateImportRequest(request);
		});
	}

	@Test
	void validatePayloadPathNotWellFormed() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/not_well_formed.xml");
			myceliumRequestService.validateImportRequest(request);
		});
	}

}
