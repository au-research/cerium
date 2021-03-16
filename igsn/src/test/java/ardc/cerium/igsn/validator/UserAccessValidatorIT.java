package ardc.cerium.igsn.validator;

import ardc.cerium.igsn.KeycloakIntegrationTest;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.igsn.model.IGSNAllocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccessValidatorIT extends KeycloakIntegrationTest {

	@Autowired
	private KeycloakService kcService;

	@Test
	@DisplayName("match the IGSN allocation to a given identifier value")
	public void matchidentifier_with_Allocation() throws Exception {
		String identifier = "20.500.11812/XXZT1" + UUID.randomUUID().toString();
		String allocationId = resourceID;
		// it's an IGSN allocationID so it will return an IGSNAllocation
		IGSNAllocation a = (IGSNAllocation) kcService.getAllocationByResourceID(allocationId);
		assertThat(a.getType()).isEqualTo("urn:ardc:igsn:allocation");
		String prefix = a.getPrefix();
		String namespace = a.getNamespace();
		assertThat(identifier.startsWith(prefix + "/" + namespace));
	}

}