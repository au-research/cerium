package ardc.cerium.igsn.validator;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.ValidationService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ForbiddenOperationException;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class PayloadValidatorTest {

	@Autowired
	SchemaService schemaService;

	@Autowired
	ValidationService validationService;

	@Autowired
	IdentifierService identifierService;

	@Autowired
	VersionService versionService;

	@Test
	@DisplayName("User has no access to the identifier")
	void validateMintPayload() throws IOException {

		// proper ardcv1
		String payload = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		User user = TestHelper.mockUser(); // mocked user with no permission

		PayloadValidator validator = new PayloadValidator(schemaService, validationService, identifierService,
				versionService);

		// mocked user has no access to the identifier in the sample,
		// result in a ForbiddenOperationException
		Assert.assertThrows(ForbiddenOperationException.class, () -> validator.validateMintPayload(payload, user));
	}

}