package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.RequestRepository;
import ardc.cerium.core.common.repository.specs.RequestSpecification;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RequestNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ApplicationProperties.class)
@ContextConfiguration(
		classes = { RequestService.class, ApplicationProperties.class, RequestMapper.class, ModelMapper.class })
@TestPropertySource("classpath:application.properties")
class RequestServiceTest {

	@Autowired
	RequestService requestService;

	@Autowired
	RequestMapper requestMapper;

	@Autowired
	ApplicationProperties applicationProperties;

	@MockBean
	RequestRepository requestRepository;

	@Test
	@DisplayName("When findOwnedById, throws RequestNotFound when the uuid is not found")
	void findOwnedById_throwsRequestNotFound() {
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		Assert.assertThrows(RequestNotFoundException.class, () -> {
			requestService.findOwnedById(UUID.randomUUID().toString(), TestHelper.mockUser());
		});
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("When findOwnedById, throws ForbiddenOperation when the user id doesn't match the creator id")
	void findOwnedById_throwsForbiddenOperation() {
		User user = TestHelper.mockUser();
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.of(TestHelper.mockRequest()));
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			requestService.findOwnedById(UUID.randomUUID().toString(), user);
		});
	}

	@Test
	@DisplayName("Find owned by ID returns correct request")
	void findOwnedById() {
		User user = TestHelper.mockUser();
		Request request = TestHelper.mockRequest();
		request.setId(UUID.randomUUID());
		request.setCreatedBy(user.getId());
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.of(request));

		assertThat(requestService.findOwnedById(request.getId().toString(), user)).isNotNull();
		assertThat(requestService.findOwnedById(request.getId().toString(), user)).isInstanceOf(Request.class);
		assertThat(requestService.findOwnedById(request.getId().toString(), user)).isSameAs(request);
	}

	@Test
	@DisplayName("FindById returns correct Request by ID")
	void findById() {
		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		assertThat(requestService.findById(UUID.randomUUID().toString())).isNull();

		when(requestRepository.findById(any(UUID.class))).thenReturn(Optional.of(TestHelper.mockRequest()));
		assertThat(requestService.findById(UUID.randomUUID().toString())).isNotNull();
		assertThat(requestService.findById(UUID.randomUUID().toString())).isInstanceOf(Request.class);
	}

	@Test
	@DisplayName("Search calls findAll in repository")
	void search() {
		requestService.search(new RequestSpecification(), PageRequest.of(0, 10));
		verify(requestRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	@DisplayName("RequestService can provide a Logger instance for each Request, and can close them on demand")
	void loggingTest() throws IOException {
		Request request = TestHelper.mockRequest();
		request.setId(UUID.randomUUID());
		String loggerName = requestService.getLoggerNameFor(request);

		Logger logger = requestService.getLoggerFor(request);
		assertThat(logger).isInstanceOf(Logger.class);
		assertThat(requestService.getLoggerFor(request)).isSameAs(logger);
		Map<String, Appender> appenders = logger.getAppenders();
		assertThat(appenders).hasSize(1);
		assertThat(appenders.get(loggerName)).isInstanceOf(FileAppender.class);
		logger.info("Test");

		// test that the log file is created and it contains the Test log
		File logFile = new File(requestService.getLoggerPathFor(request));
		assertThat(logFile).exists();
		String fileContent = Helpers.readFile(requestService.getLoggerPathFor(request));
		assertThat(fileContent).contains("Test");

		// closing the log file
		requestService.closeLoggerFor(request);

		// by trying to obtain another logger for the same name, it creates a default
		// logger instead, not the same one with the appenders
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		Logger otherLogger = context.getLogger(requestService.getLoggerNameFor(request));
		Map<String, Appender> otherLoggerAppenders = otherLogger.getAppenders();
		otherLogger.info("Test2");
		assertThat(otherLoggerAppenders.get(loggerName)).isNull();

		// clean up
		(new File(applicationProperties.getDataPath() + "/requests/" + request.getId())).deleteOnExit();
	}

	@Test
	@DisplayName("Create returns new instance of Request with directory created")
	void create() {
		User user = TestHelper.mockUser();
		RequestDTO dto = new RequestDTO();
		dto.setType("Test");

		Request mockSavedRequest = TestHelper.mockRequest();
		mockSavedRequest.setId(UUID.randomUUID());
		when(requestRepository.save(any(Request.class))).thenReturn(mockSavedRequest);

		Request actual = requestService.create(dto, user);

		// save is called
		verify(requestRepository, times(1)).save(any(Request.class));
		verify(requestRepository, times(1)).saveAndFlush(any(Request.class));

		// data path attribute is set
		assertThat(actual.getAttribute(Attribute.DATA_PATH)).isNotBlank();
		assertThat(actual.getCreatedAt()).isNotNull();

		// data path is created
		assertThat(new File(requestService.getDataPathFor(actual))).isDirectory();

		(new File(requestService.getDataPathFor(actual))).deleteOnExit();
	}

	@Test
	@DisplayName("Update validate request ownership by createdBy")
	void update_failByOwnership() {
		Request request = TestHelper.mockRequest();
		RequestDTO dto = new RequestDTO();
		dto.setStatus("Running");

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			requestService.update(request, dto, TestHelper.mockUser());
		});
	}

	@Test
	@DisplayName("Update successfully sets status and type")
	void update() {
		User user = TestHelper.mockUser();
		Request request = TestHelper.mockRequest();
		request.setStatus(Request.Status.ACCEPTED);
		request.setCreatedBy(user.getId());
		RequestDTO dto = new RequestDTO();
		dto.setStatus(Request.Status.RUNNING.toString());
		dto.setType("NewType");

		Request actual = requestService.update(request, dto, user);
		verify(requestRepository, times(1)).saveAndFlush(any(Request.class));

		assertThat(actual.getType()).isEqualTo("NewType");
		assertThat(actual.getStatus()).isEqualTo(Request.Status.RUNNING);
	}

}