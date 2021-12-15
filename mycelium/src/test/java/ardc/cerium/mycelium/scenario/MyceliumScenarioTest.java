package ardc.cerium.mycelium.scenario;

import ardc.cerium.mycelium.MyceliumIntegrationTest;
import ardc.cerium.mycelium.service.MyceliumService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "2000")
public abstract class MyceliumScenarioTest extends MyceliumIntegrationTest {

	public static String importRecordAPI = "/api/services/mycelium/import-record";

	public static String indexRecordAPI = "/api/services/mycelium/index-record";

	@Autowired
	public WebTestClient webTestClient;

	@Autowired
	MyceliumService myceliumService;

}
