package ardc.cerium.oai.response;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.oai.service.OAIPMHService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OAIPMHService.class, ApplicationProperties.class, SchemaService.class })
@TestPropertySource(properties = "app.oai.enabled=true")
public class OAIIdentifyTest {

	@Autowired
	OAIPMHService service;

	@MockBean
	VersionService versionService;

	@MockBean
	RecordService recordService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	SchemaService schemaService;

	@Test
	void identify() {
		OAIIdentifyResponse response = (OAIIdentifyResponse) service.identify();
		assertThat(response).isInstanceOf(OAIResponse.class);
		assertThat(response.getIdentify().getRepositoryName()).isEqualTo(applicationProperties.getName());
	}

}
