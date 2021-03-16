package ardc.cerium.igsn;

import ardc.cerium.core.common.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.xml.bind.DatatypeConverter;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "10000")
public abstract class WebIntegrationTest extends IntegrationTest {

	@Autowired
	public WebTestClient webTestClient;

	@Autowired
	private IdentifierRepository identifierRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Autowired
	private VersionRepository versionRepository;

	@Autowired
	private URLRepository urlRepository;

	@Autowired
	private EmbargoRepository embargoRepository;

	public String getBasicAuthenticationHeader(String username, String password) {
		return "Basic " + DatatypeConverter.printBase64Binary((username + ":" + password).getBytes());
	}

	@AfterEach
	void cleanUp() {
		try {
			urlRepository.deleteAll();
			versionRepository.deleteAll();
			identifierRepository.deleteAll();
			embargoRepository.deleteAll();
			recordRepository.deleteAll();

		}
		catch(Exception e)
		{
			System.out.print(e.getMessage());
		}
	}

}
