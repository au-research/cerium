package ardc.cerium.core;

import ardc.cerium.core.common.entity.*;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.charset.Charset;
import java.util.*;

public class TestHelper {

	public static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A helpful method to stub out a Record for testing
	 * @return a record with random things
	 */
	public static Record mockRecord() {
		Record record = new Record();
		record = populateWithOwner(record, UUID.randomUUID());
		record.setIdentifiers(new ArrayList<>());
		record.setVersions(new HashSet<>());
		record.setCurrentVersions(new ArrayList<>());
		return record;
	}

	/**
	 * Mock a record with a predefined UUID (not used for persisting)
	 * @param randomUUID some provided UUID
	 * @return a record with random things
	 */
	public static Record mockRecord(UUID randomUUID) {
		Record record = new Record(randomUUID);
		record = populateWithOwner(record, UUID.randomUUID());
		record.setIdentifiers(new ArrayList<>());
		record.setVersions(new HashSet<>());
		record.setCurrentVersions(new ArrayList<>());
		return record;
	}

	public static Record populateWithOwner(Record record, UUID creatorID) {
		record.setCreatorID(creatorID);
		record.setAllocationID(UUID.randomUUID());
		record.setOwnerID(creatorID);
		record.setOwnerType(Record.OwnerType.User);
		record.setModifiedAt(new Date());
		record.setModifierID(creatorID);
		record.setCreatedAt(new Date());
		return record;
	}

	/**
	 * Mock a version
	 * @return a Version with a mocked up record
	 */
	public static Version mockVersion() {
		Record record = mockRecord();
		Version version = new Version(UUID.randomUUID());
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setCreatorID(UUID.randomUUID());
		version.setRecord(record);
		version.setSchema("test-schema");
		version.setRequestID(UUID.randomUUID());
		return version;
	}

	/**
	 * Mock a version
	 * @return a Version with a mocked up record
	 */
	public static Version mockVersion(UUID id) {
		Record record = mockRecord(id);
		Version version = new Version(UUID.randomUUID());
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setCreatorID(UUID.randomUUID());
		version.setRecord(record);
		version.setSchema("test-schema");
		return version;
	}

	/**
	 * Mock a version given a record
	 * @param record the record that the version will belong to
	 * @return a Version
	 */
	public static Version mockVersion(Record record) {
		Version version = new Version();
		version.setCreatedAt(new Date());
		version.setCurrent(true);
		version.setRecord(record);
		version.setCreatorID(record.getCreatorID());
		version.setContent("random".getBytes());
		return version;
	}

	/**
	 * Mock an identifier given a record
	 * @param record the record that the identifier will belong to
	 * @return a Identifier
	 */
	public static Identifier mockIdentifier(Record record) {
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.ACCESSIBLE);
		identifier.setRecord(record);
		return identifier;
	}

	/**
	 * Mock an identifier
	 * @return an Identifier with a mocked up record
	 */
	public static Identifier mockIdentifier(UUID id) {
		Record record = mockRecord(id);
		Identifier identifier = new Identifier(UUID.randomUUID());
		identifier.setCreatedAt(new Date());
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.ACCESSIBLE);
		identifier.setRecord(record);
		return identifier;
	}

	/**
	 * Mock a version
	 * @return an Identifier with a mocked up record
	 */
	public static Identifier mockIdentifier() {
		Record record = mockRecord();
		Identifier identifier = new Identifier();
		identifier.setCreatedAt(new Date());
		identifier.setType(Identifier.Type.IGSN);
		identifier.setStatus(Identifier.Status.ACCESSIBLE);
		identifier.setRecord(record);
		return identifier;
	}

	/**
	 * Mock a url
	 * @return a url with a mocked up record
	 */
	public static URL mockUrl() {
		Record record = mockRecord();
		URL url = new URL(UUID.randomUUID());
		url.setCreatedAt(new Date());
		url.setResolvable(false);
		url.setRecord(record);
		return url;
	}

	/**
	 * Mock a url
	 * @return a url with a record
	 */
	public static URL mockUrl(Record record) {
		URL url = new URL();
		url.setCreatedAt(new Date());
		url.setResolvable(false);
		url.setRecord(record);
		return url;
	}

	/**
	 * Mock a url
	 * @return a url with a record
	 */
	public static URL mockUrl(UUID id) {
		Record record = mockRecord();
		URL url = new URL(id);
		url.setCreatedAt(new Date());
		url.setResolvable(false);
		url.setRecord(record);
		return url;
	}

	public static User mockUser() {
		User user = new User(UUID.randomUUID());
		user.setName("John Wick");
		user.setEmail("jwick@localhost.com");
		user.setDataCenters(new ArrayList<>());
		user.setAllocations(new ArrayList<>());
		return user;

		// mock a user resources
	}

	public static DataCenter mockDataCenter() {
		DataCenter dataCenter = new DataCenter(UUID.randomUUID());
		return dataCenter;

		// mock a user resources
	}


	public static Request mockRequest() {
		Request request = new Request();
		request.setId(UUID.randomUUID());
		request.setStatus(Request.Status.ACCEPTED);
		request.setCreatedAt(new Date());
		request.setCreatedBy(UUID.randomUUID());
		return request;
	}

	public static Logger getConsoleLogger(String loggerName, Level level) {
		// get the current Logging context and Configuration
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		org.apache.logging.log4j.core.config.Configuration configuration = context.getConfiguration();
		LoggerConfig loggerConfig = new LoggerConfig(loggerName, level, false);

		// build a PatternLayout to be used with logging
		String pattern = "[%d{ISO8601}][%-5p][%c{2}] %m%n";
		PatternLayout.Builder builder = PatternLayout.newBuilder().withPattern(pattern)
				.withCharset(Charset.defaultCharset()).withAlwaysWriteExceptions(false).withNoConsoleNoAnsi(false);
		PatternLayout layout = builder.build();

		// build the appender and add them to the loggerConfig
		Appender appender = ConsoleAppender.newBuilder().setName(loggerName).setLayout(layout)
				.setConfiguration(configuration).withImmediateFlush(true).withBufferSize(8192).build();
		appender.start();

		loggerConfig.addAppender(appender, level, null);

		// add a new logger with the provided config
		configuration.addLogger(loggerName, loggerConfig);

		// update all the loggers to make sure this logger by name is available everywhere
		context.updateLoggers();
		return context.getLogger(loggerName);
	}

	/**
	 * Mock an embargo given a record
	 * @param record the record that the embargo will belong to
	 * @return a Embargo
	 */
	public static Embargo mockEmbargo(Record record) {
		Embargo embargo = new Embargo();
		embargo.setEmbargoEnd(new Date());
		embargo.setRecord(record);
		return embargo;
	}

	/**
	 * Mock a version
	 * @return an Identifier with a mocked up record
	 */
	public static Embargo mockEmbargo() {
		Record record = mockRecord();
		Embargo embargo = new Embargo(UUID.randomUUID());
		embargo.setEmbargoEnd(new Date());
		embargo.setRecord(record);
		return embargo;
	}

	public static String getRandomIdentifierValue(String prefix, String allocation){
		String suffix = RandomStringUtils.random(10, true, true);
		return String.format("%s/%s%s", prefix, allocation,suffix).toUpperCase();
	}

}
