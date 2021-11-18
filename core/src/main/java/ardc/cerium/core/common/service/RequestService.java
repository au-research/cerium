package ardc.cerium.core.common.service;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.RequestRepository;
import ardc.cerium.core.common.repository.specs.RequestSpecification;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RequestNotFoundException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class RequestService {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RequestService.class);

	final RequestRepository requestRepository;

	final ApplicationProperties applicationProperties;

	final RequestMapper requestMapper;

	private Map<String, Logger> loggers;

	public RequestService(RequestRepository requestRepository, ApplicationProperties applicationProperties,
			RequestMapper requestMapper) {
		this.requestRepository = requestRepository;
		this.applicationProperties = applicationProperties;
		this.requestMapper = requestMapper;
		loggers = new HashMap<>();
	}

	/**
	 * Find a request by id, created by a given user
	 * @param id uuid of the request
	 * @param user the creator {@link User}
	 * @return the {@link Request}
	 * @throws RequestNotFoundException when the request is not found
	 * @throws ForbiddenOperationException when the request is not owned by the user
	 */
	public Request findOwnedById(String id, User user) throws RequestNotFoundException, ForbiddenOperationException {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		Request request = opt.orElseThrow(() -> new RequestNotFoundException(id));

		if (!request.getCreatedBy().equals(user.getId())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		return request;
	}

	/**
	 * Finds a {@link Request} by it's id
	 * @param id the UUID string
	 * @return the {@link Request}
	 */
	public Request findById(String id) {
		Optional<Request> opt = requestRepository.findById(UUID.fromString(id));
		return opt.orElse(null);
	}

	/**
	 * Performs a search based on the predefined search specification
	 * @param specs an instance of {@link RequestSpecification}
	 * @param pageable an instance of {@link Pageable}
	 * @return a {@link Page} of {@link Request}
	 */
	public Page<Request> search(Specification<Request> specs, Pageable pageable) {
		return requestRepository.findAll(specs, pageable);
	}

	/**
	 * Get an instance of {@link Logger} for use with a {@link Request}. The Service will
	 * create a new Logger instance if required, and store them within {@link #loggers}
	 * map, return them as needed
	 * @param request the {@link Request} to create/use a logger from
	 * @return the {@link Logger} instance
	 */
	public Logger getLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		if (loggers.containsKey(loggerName)) {
			return loggers.get(loggerName);
		}
		Logger logger = createLoggerFor(request);
		loggers.put(loggerName, logger);
		return logger;
	}

	/**
	 * Build a new {@link Logger} instance for the provided {@link Request}
	 * @param request the {@link Request} to create a logger from
	 * @return the {@link Logger} with proper FileAppender
	 */
	private Logger createLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		String loggerPath = getLoggerPathFor(request);

		// get the current Logging context and Configuration
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		Configuration configuration = context.getConfiguration();
		LoggerConfig loggerConfig = new LoggerConfig(loggerName, Level.INFO, false);

		// build a PatternLayout to be used with logging
		String pattern = "[%d{ISO8601}][%-5p][%c{2}] %m%n";
		PatternLayout.Builder builder = PatternLayout.newBuilder().withPattern(pattern)
				.withCharset(Charset.defaultCharset()).withAlwaysWriteExceptions(false).withNoConsoleNoAnsi(false);
		PatternLayout layout = builder.build();

		// build the appender and add them to the loggerConfig
		Appender appender = FileAppender.newBuilder().setName(loggerName).setLayout(layout).withFileName(loggerPath)
				.withAppend(true).setConfiguration(configuration).withLocking(false).withImmediateFlush(true)
				.withBufferSize(8192).withAdvertise(false).withAdvertiseUri("").build();
		appender.start();
		loggerConfig.addAppender(appender, Level.INFO, null);

		// add a new logger with the provided config
		configuration.addLogger(loggerName, loggerConfig);

		// update all the loggers to make sure this logger by name is available everywhere
		context.updateLoggers();
		return context.getLogger(loggerName);
	}

	/**
	 * Returns the configured logger path for a {@link Request}
	 * @param request the {@link Request}
	 * @return the absolute String path to the log file
	 */
	public String getLoggerPathFor(Request request) {
		String separator = System.getProperty("file.separator");
		return getDataPathFor(request) + separator + "logs";
	}

	/**
	 * Return the official data path for a given {@link Request}
	 * @param request the {@link Request}
	 * @return absolute path to the {@link Request}
	 */
	public String getDataPathFor(Request request) {
		String separator = System.getProperty("file.separator");
		return applicationProperties.getDataPath() + separator + "requests" + separator + request.getId();
	}

	/**
	 * Returns the name of the {@link Logger} instance for a given {@link Request}
	 * @param request the {@link Request}
	 * @return the String name of the Logger for use globally
	 */
	public String getLoggerNameFor(Request request) {
		return "Request." + request.getId();
	}

	/**
	 * Close the logger by {@link Request} Use to remove all appenders from the Logger
	 * instance and reduce memory footprint
	 * @param request the {@link Request}
	 */
	public void closeLoggerFor(Request request) {
		String loggerName = getLoggerNameFor(request);
		LoggerContext context = (LoggerContext) LogManager.getContext(true);
		Configuration configuration = context.getConfiguration();
		context.getLogger(loggerName).getAppenders()
				.forEach((s, appender) -> {
					appender.stop();
					context.getLogger(loggerName).removeAppender(appender);
				});
		configuration.removeLogger(loggerName);
		context.updateLoggers();
		loggers.remove(loggerName);
	}

	/**
	 * Creates a new Request
	 * @param dto the {@link RequestDTO} representation of the request
	 * @param user the {@link User} that creates the request
	 * @return the {@link Request} that was created and persisted
	 */
	public Request create(RequestDTO dto, User user) {
		// todo validation
		Request request = requestMapper.getConverter().reverse().convert(dto);
		if (request == null) {
			throw new RuntimeException("Failed to convert Request DTO");
		}
		request.setCreatedBy(user.getId());
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());

		// save to obtain the request.id
		request = requestRepository.save(request);

		// create request directory
		try {
			Path path = Paths.get(getDataPathFor(request));
			logger.info("Creating data path: {}", path.toAbsolutePath());
			Files.createDirectories(path);
			logger.info("Created data path: {}", path.toAbsolutePath());
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			logger.error("Failed creating data path {}", e.getMessage());
		}

		// create and close logger, so that the log file get created
		getLoggerFor(request);
		closeLoggerFor(request);

		requestRepository.saveAndFlush(request);
		return request;
	}

	public Request update(Request entity, RequestDTO dto, User user) {
		// validate request ownership
		if (!entity.getCreatedBy().equals(user.getId())) {
			throw new ForbiddenOperationException("You don't own this request");
		}

		// updates these allowed fields
		entity.setType(dto.getType() != null ? dto.getType() : entity.getType());
		entity.setStatus(dto.getStatus() != null ? Request.Status.valueOf(dto.getStatus()) : entity.getStatus());

		entity.setUpdatedAt(new Date());
		requestRepository.saveAndFlush(entity);
		return entity;
	}

	// todo delete

	public Request save(Request request) {
		request.setUpdatedAt(new Date());
		return requestRepository.saveAndFlush(request);
	}

}
