package ardc.cerium.igsn.service;

import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.*;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.*;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.VersionContentAlreadyExistsException;
import ardc.cerium.core.exception.VersionIsOlderThanCurrentException;
import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ImportService.class, SchemaService.class })
@TestPropertySource(properties="app.igsn.enabled=true")
class ImportServiceTest {

	@Autowired
	ImportService importService;

	@MockBean
	IGSNRequestService igsnRequestService;

	@MockBean
	IdentifierService identifierService;

	@MockBean
	RecordService recordService;

	@MockBean
	IGSNVersionService igsnVersionService;

	@MockBean
	URLService urlService;

	@MockBean
	EmbargoService embargoService;

	@Test
	@DisplayName("Import valid payload and valid request")
	void importRequest_validCreation() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		Request request = TestHelper.mockRequest();
		String ownerId = UUID.randomUUID().toString();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.OWNER_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.importRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		// 1 of each are created
		verify(recordService, times(1)).save(any(Record.class));
		verify(identifierService, times(1)).save(any(Identifier.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
	}

	@Test
	@DisplayName("Import valid payload but error creating Identifier should returns null and delete Record")
	void importRequest_errorCreatingIdentifier_returnsNull() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));
		when(identifierService.save(any(Identifier.class))).thenThrow(new ForbiddenOperationException("bad"));

		Request request = TestHelper.mockRequest();
		String ownerId = UUID.randomUUID().toString();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.OWNER_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.importRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isNull();

		// record is saved first, then deleted
		verify(recordService, times(1)).save(any(Record.class));
		verify(recordService, times(1)).delete(any(Record.class));
	}

	@Test
	@DisplayName("Update valid payload calls the right save method")
	void updateRequest() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		version.setCreatedAt(new Date());
		record.setCurrentVersions(Collections.singletonList(version));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		String ownerId = UUID.randomUUID().toString();
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		verify(recordService, times(1)).save(any(Record.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
	}

	@Test
	@DisplayName("Update similar content already exists throws VersionContentAlreadyExistsException")
	void updateRequest_previousVersionSameHash_throwsException() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		String ownerId = UUID.randomUUID().toString();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version oldVersion = TestHelper.mockVersion(record);
		oldVersion.setRequestID(UUID.randomUUID());
		oldVersion.setSchema(SchemaService.ARDCv1);
		oldVersion.setHash(VersionService.getHash(Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml")));
		record.setCurrentVersions(Collections.singletonList(oldVersion));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.CREATOR_ID, ownerId);

		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> {
			importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);
		});

	}

	@Test
	@DisplayName("Update with an older version throws VersionIsOlderThanCurrentException")
	void updateRequest_previousVersionNewer_throwsException() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		String ownerId = UUID.randomUUID().toString();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version oldVersion = TestHelper.mockVersion(record);
		oldVersion.setSchema(SchemaService.ARDCv1);
		oldVersion.setRequestID(UUID.randomUUID());
		oldVersion.setHash(VersionService.getHash(Helpers.readFile("src/test/resources/xml/sample_ardcv1.xml")));
		Calendar calendar = Calendar.getInstance();
		// make the current version 7 days newer
		calendar.add(Calendar.DATE, 7);
		oldVersion.setCreatedAt(calendar.getTime());
		record.setCurrentVersions(Collections.singletonList(oldVersion));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.CREATOR_ID, ownerId);

		Assert.assertThrows(VersionIsOlderThanCurrentException.class, () -> {
			importService.updateRequest(new File("src/test/resources/xml/older_sample_ardcv1.xml"), request);
		});

	}

	@Test
	@DisplayName("Import valid payload with embargo and valid request")
	void importRequest_validCreation_withEmbargo() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		Request request = TestHelper.mockRequest();
		String ownerId = UUID.randomUUID().toString();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.OWNER_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.importRequest(new File("src/test/resources/xml/sample_ardcv1_embargoEnd.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		// 1 of each are created
		verify(recordService, times(1)).save(any(Record.class));
		verify(identifierService, times(1)).save(any(Identifier.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
		verify(embargoService, times(1)).save(any(Embargo.class));
	}

	@Test
	@DisplayName("Update valid payload with embargo calls the right save method")
	void updateRequestEmbargo() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		String ownerId = UUID.randomUUID().toString();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		version.setCreatedAt(new Date());
		record.setCurrentVersions(Collections.singletonList(version));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1_embargoEnd.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		verify(recordService, times(1)).save(any(Record.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
		verify(embargoService, times(1)).save(any(Embargo.class));
	}

	@Test
	@DisplayName("Update valid payload with embargo calls the right save method then update again with embargo removed")
	void updateRequestToNoEmbargo() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		String ownerId = UUID.randomUUID().toString();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		version.setCreatedAt(new Date());
		record.setCurrentVersions(Collections.singletonList(version));
		Embargo embargo = TestHelper.mockEmbargo(record);

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1_embargoEnd.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		verify(recordService, times(1)).save(any(Record.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
		verify(embargoService, times(1)).save(any(Embargo.class));

		when(embargoService.findByRecord(record)).thenReturn(embargo);
		assertThat(embargo).isInstanceOf(Embargo.class);
		UUID embargoId = embargo.getId();

		// then importRequest again with valid payload with embargo removed and a valid request
		Identifier resultNoEmbargo = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1.xml"), request);
		assertThat(resultNoEmbargo).isInstanceOf(Identifier.class);
		verify(embargoService, times(1)).delete(embargoId);
	}

	@Test
	@DisplayName("Import valid payload but error creating Identifier should returns null and delete Record")
	void reserveRequest_Test() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		Request request = TestHelper.mockRequest();
		String ownerId = UUID.randomUUID().toString();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.OWNER_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		for(int i = 0; i< 10 ; i++){
			String identifierValue = TestHelper.getRandomIdentifierValue("20.500.11812", "XXZT1");
			System.out.print(identifierValue+ "\n");
			Identifier result = importService.reserveRequest(identifierValue, request);
			assertThat(result.getValue().equals(identifierValue));
		}
		verify(recordService, times(10)).save(any(Record.class));

	}

	@Test
	@DisplayName("Update valid payload with embargo in the past does not call embargo save method")
	void updateRequestEmbargoInPast() throws IOException {
		when(igsnRequestService.getLoggerFor(any(Request.class)))
				.thenReturn(TestHelper.getConsoleLogger(ImportServiceTest.class.getName(), Level.DEBUG));

		// given an existing Identifier and an existing Record
		String ownerId = UUID.randomUUID().toString();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(UUID.fromString(ownerId));
		record.setOwnerType(Record.OwnerType.User);
		Identifier identifier = TestHelper.mockIdentifier(record);
		identifier.setValue("10273/XX0TUIAYLV");
		Version version = TestHelper.mockVersion(record);
		version.setSchema(SchemaService.ARDCv1);
		version.setCreatedAt(new Date());
		record.setCurrentVersions(Collections.singletonList(version));

		when(identifierService.findByValueAndType(identifier.getValue(), Identifier.Type.IGSN)).thenReturn(identifier);

		Request request = TestHelper.mockRequest();
		request.setAttribute(Attribute.OWNER_TYPE, "User");
		request.setAttribute(Attribute.CREATOR_ID, ownerId);
		request.setAttribute(Attribute.ALLOCATION_ID, UUID.randomUUID().toString());

		// when importRequest a valid payload and a valid request
		Identifier result = importService.updateRequest(new File("src/test/resources/xml/sample_ardcv1_embargoEndPast.xml"), request);

		assertThat(result).isInstanceOf(Identifier.class);

		verify(recordService, times(2)).save(any(Record.class));
		verify(igsnVersionService, times(1)).save(any(Version.class));
		verify(embargoService, times(0)).save(any(Embargo.class));
	}


}