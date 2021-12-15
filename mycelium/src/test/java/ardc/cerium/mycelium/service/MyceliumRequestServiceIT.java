package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.MyceliumIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class MyceliumRequestServiceIT extends MyceliumIntegrationTest {

	@Autowired
	MyceliumRequestService myceliumRequestService;

    @Test
	void itCreatesRequestsFromDTO() {
		// when create a request
		RequestDTO dto = new RequestDTO();
		dto.setType("test");
		Request actual = myceliumRequestService.createRequest(dto);

		// the request is created properly
		assertThat(actual).isNotNull();
		assertThat(actual.getStatus()).isEqualTo(Request.Status.CREATED);
	}
}
