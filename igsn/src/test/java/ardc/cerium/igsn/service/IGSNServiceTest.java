package ardc.cerium.igsn.service;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.igsn.config.IGSNApplicationConfig;
import ardc.cerium.igsn.model.IGSNAllocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { IGSNService.class, SchemaService.class, IGSNApplicationConfig.class })
@TestPropertySource(properties="app.igsn.enabled=true")
class IGSNServiceTest {

	@Autowired
	IGSNService igsnService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	ImportService importService;

	@MockBean
	VersionService versionService;

	@MockBean
	IGSNRegistrationService igsnRegistrationService;

	@BeforeEach
	void beforeEach() {
		igsnService.init();
	}

	@Test
	@DisplayName("Get IGSNAllocation gives null when user has the wrong allocation")
	void getIGSNAllocationForContent_mismatchUser_null() throws IOException {

		// given XML has 10273/XX0TUIAYLV
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		// given user that has allocation 20.500.11812/XXAA
		User user = TestHelper.mockUser();
		Allocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE));
		user.setAllocations(Arrays.asList(allocation));

		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.CREATE)).isEqualTo(null);
	}

	@Test
	@DisplayName("Get IGSNAllocation returns proper IGSNAllocation when the prefix and namespace matches")
	void getIGSNAllocationForContent() throws IOException {

		// given XML has 10273/XX0TUIAYLV
		String xml = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");

		// given user that has allocation 20.500.11812/XXAA
		User user = TestHelper.mockUser();
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.getAttributes().put("prefix", Collections.singletonList("10273"));
		allocation.getAttributes().put("namespace", Collections.singletonList("XX0TUIAYLV"));
		allocation.setAttributes(allocation.getAttributes());
		allocation.setIGSNAllocationAttributes();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.CREATE)).isInstanceOf(IGSNAllocation.class);
		assertThat(igsnService.getIGSNAllocationForContent(xml, user, Scope.UPDATE)).isInstanceOf(IGSNAllocation.class);
	}

}