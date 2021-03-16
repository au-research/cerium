package ardc.cerium.igsn.service;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.*;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.service.*;
import ardc.cerium.core.common.transform.TransformerFactory;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.NotFoundException;
import ardc.cerium.igsn.model.IGSNAllocation;
import ardc.cerium.igsn.transform.ardcv1.ARDCv1ToRegistrationMetadataTransformer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = { IGSNRegistrationService.class, SchemaService.class })
class IGSNRegistrationServiceIT {

	public static MockWebServer mockMDS;

	@BeforeAll
	static void setUp() throws IOException {
		mockMDS = new MockWebServer();
		mockMDS.start();
	}

	@AfterAll
	static void tearDown() throws IOException {
		mockMDS.shutdown();
	}

	@Autowired
	IGSNRegistrationService igsnRegistrationService;

	@Autowired
	SchemaService schemaService;

	@MockBean
	KeycloakService keycloakService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	IGSNVersionService igsnVersionService;

	@MockBean
	URLService urlService;

	@Test
	public void throws_exception_if_identifier_doesnt_exist() throws Exception {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setType(IGSNService.EVENT_MINT);
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			igsnRegistrationService.registerIdentifier(identifierValue, request);
		});

	}

	@Test
	public void throws_exception_if_version_doesnt_exist() throws Exception {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setType(IGSNService.EVENT_UPDATE);
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);
		Assert.assertThrows(NotFoundException.class, () -> {
			igsnRegistrationService.registerIdentifier(identifierValue, request);
		});

	}

	@Test
	public void updates_url_and_registration_metadata() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		request.setType(IGSNService.EVENT_UPDATE);
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.PENDING);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());
		record.setCurrentVersions(Collections.singletonList(version));

		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		igsnRegistrationService.registerIdentifier(identifierValue, request);
		Identifier createdIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);
		assertEquals(Identifier.Status.ACCESSIBLE, createdIdentifier.getStatus());

	}

	@Test
	public void updates_registration_metadata_only() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		request.setType(IGSNService.EVENT_UPDATE);

		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.ACCESSIBLE);
		identifier.setValue(identifierValue);
		identifier.setRequestID(UUID.randomUUID());
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());

		URL url = new URL();
		url.setRecord(record);
		url.setUrl("https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(urlService.findByRecord(any(Record.class))).thenReturn(url);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		igsnRegistrationService.registerIdentifier(identifierValue, request);
		Identifier createdIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		assertEquals(Identifier.Status.ACCESSIBLE, createdIdentifier.getStatus());
	}


	@Test
	public void updates_identifier_and_registration_metadata_if_Identifier_status_is_PENDING() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		request.setType(IGSNService.EVENT_UPDATE);

		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.PENDING);
		identifier.setValue(identifierValue);
		identifier.setRequestID(UUID.randomUUID());
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());

		URL url = new URL();
		url.setRecord(record);
		url.setUrl("https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(urlService.findByRecord(any(Record.class))).thenReturn(url);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		mockMDS.enqueue(new MockResponse().setBody("OK").setResponseCode(201));
		igsnRegistrationService.registerIdentifier(identifierValue, request);
		Identifier createdIdentifier = identifierService.findByValueAndType(identifierValue, Identifier.Type.IGSN);

		assertEquals(Identifier.Status.ACCESSIBLE, createdIdentifier.getStatus());
	}





	@Test
	public void do_not_updates_once_registration_igsn_and_metadata_is_latest() throws Exception {
		String mockedServerURL = String.format("http://localhost:%s", mockMDS.getPort());
		IGSNAllocation allocation = TestHelper.mockIGSNAllocation();
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.UPDATE));
		allocation.setPrefix("20.500.11812");
		allocation.setNamespace("XXZT1");
		allocation.setName("Mocked up test Allocation");
		allocation.setMds_username("ANDS.IGSN");
		allocation.setMds_url(mockedServerURL);

		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, UUID.randomUUID().toString());
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());
		request.setType(IGSNService.EVENT_UPDATE);
		String identifierValue = "10273/XX0TUIAYLV";
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setType(Identifier.Type.IGSN);
		identifier.setValue(identifierValue);
		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		String content = Helpers.readFile("src/test/resources/xml/sample_ardcv1_destroyed.xml");
		version.setContent(content.getBytes());
		version.setHash(VersionService.getHash(content));
		version.setCreatedAt(request.getCreatedAt());

		URL url = new URL();
		url.setRecord(record);
		url.setUrl("https://demo.identifiers.ardc.edu.au/igsn/#/meta/XX0TUIAYLV");
		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.ARDCv1)).thenReturn(version);
		when(urlService.findByRecord(any(Record.class))).thenReturn(url);
		when(keycloakService.getAllocationByResourceID(any())).thenReturn(allocation);

		Schema fromSchema = schemaService.getSchemaByID(SchemaService.ARDCv1);
		Schema toSchema = schemaService.getSchemaByID(SchemaService.IGSNREGv1);

		ARDCv1ToRegistrationMetadataTransformer transformer = (ARDCv1ToRegistrationMetadataTransformer) TransformerFactory
				.create(fromSchema, toSchema);
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(tz);
		String utcDateTimeStr = df.format(version.getCreatedAt());
		transformer.setParam("timeStamp", utcDateTimeStr).setParam("registrantName", allocation.getMds_username());

		Version regVersion = transformer.transform(version);
		regVersion.setCreatedAt(request.getCreatedAt());
		regVersion.setRequestID(request.getId());
		regVersion.setCurrent(true);

		when(igsnVersionService.getCurrentVersionForRecord(record, SchemaService.IGSNREGv1)).thenReturn(regVersion);

		igsnRegistrationService.registerIdentifier(identifierValue, request);

	}

}