package ardc.cerium.mycelium.service;

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
@Import({ MyceliumService.class })
public class MyceliumRequestValidationTest {

	@Autowired
	MyceliumService myceliumService;

	@MockBean
	RequestService requestService;

	@MockBean
	GraphService graphService;

	@Test
	void validatePayloadPathNotSet() {
		assertThrows(ContentNotSupportedException.class, () -> myceliumService.validateRequest(new Request()));
	}

	@Test
	void validatePayloadPathNotAccessible() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "blah");
			myceliumService.validateRequest(request);
		});
	}

	@Test
	void validatePayloadPathEmpty() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/empty.xml");
			myceliumService.validateRequest(request);
		});
	}

	@Test
	void validatePayloadPathNotWellFormed() {
		assertThrows(ContentNotSupportedException.class, () -> {
			Request request = new Request();
			request.setAttribute(Attribute.PAYLOAD_PATH, "src/test/resources/not_well_formed.xml");
			myceliumService.validateRequest(request);
		});
	}

}
