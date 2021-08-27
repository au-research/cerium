package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
* The {@link Service} that performs Mycelium operations for handling {@link Request}
*/
@Slf4j
@Service
public class MyceliumRequestService {

	public static final String IMPORT_REQUEST_TYPE = "mycelium-import";

	public static final String DELETE_REQUEST_TYPE = "mycelium-delete";

	public static final String AFFECTED_REL_REQUEST_TYPE = "mycelium-affected_relationships";

	private final RequestMapper requestMapper;

	private final RequestService requestService;

	public MyceliumRequestService(RequestMapper requestMapper, RequestService requestService) {
		this.requestMapper = requestMapper;
		this.requestService = requestService;
	}

	/**
	 * Create a {@link Request} for use with Mycelium service.
	 *
	 * Data path initialised and log file created
	 * @param requestDTO the {@link RequestDTO} to create from
	 * @return the {@link Request} created
	 */
	public Request createRequest(RequestDTO requestDTO) {
		Request request = new Request();
		request.setType(requestDTO.getType());
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());
		request.setStatus(Request.Status.CREATED);

		request = save(request);

		// data path
		try {
			Path path = Paths.get(requestService.getDataPathFor(request));
			Files.createDirectories(path);
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			log.error("Failed creating data path {}", e.getMessage());
		}

		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));
		Logger requestLogger = requestService.getLoggerFor(request);
		requestLogger.info("Request created");
		requestService.closeLoggerFor(request);

		if (request.getType().equals(IMPORT_REQUEST_TYPE)) {
			String dataPath = requestService.getDataPathFor(request);
			String payloadPath = dataPath + File.separator + "payload";
			request.setAttribute(Attribute.PAYLOAD_PATH, payloadPath);
		}

		request = save(request);

		return request;
	}

	/**
	 * Save a payload content to the data path of the Request
	 * @param request the {@link Request} to obtain the data path from
	 * @param payload the content to save
	 */
	public void saveToPayloadPath(Request request, String payload) {
		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		try {
			Files.createFile(Paths.get(payloadPath));
			Files.writeString(Paths.get(payloadPath), payload);
		}
		catch (IOException e) {
			log.error("Failed to write payload [payloadPath={}] [content={}] Reason: {}", payloadPath, payload,
					e.getMessage());
		}
	}

	/**
	 * Validate if the request is valid for creation
	 * @param requestDTO the {@link RequestDTO} to create a Request from
	 * @throws ContentNotSupportedException if the type is invalid
	 */
	public void validateRequestDTO(RequestDTO requestDTO) {
		String requestType = requestDTO.getType();

		if (requestType == null) {
			throw new ContentNotSupportedException("RequestType must be set");
		}

		if (!(requestType.equals(IMPORT_REQUEST_TYPE) || requestType.equals(DELETE_REQUEST_TYPE)
				|| requestType.equals(AFFECTED_REL_REQUEST_TYPE))) {
			throw new ContentNotSupportedException(String.format("Invalid Request Type: %s", requestType));
		}
	}

	/**
	 * Validate the Import Request
	 * @param request the {@link Request} to validate
	 * @throws ContentNotSupportedException when the payload is not valid
	 */
	public void validateImportRequest(Request request) {
		// todo migrate this method and tests to MyceliumRequestService

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		if (payloadPath == null) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// validate payload
		String payload;
		try {
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// test payload is empty
		if (payload.isBlank()) {
			throw new ContentNotSupportedException("Payload is empty");
		}

		// test for payload syntax error(s)
		try {
			JSONObject jsonObject = new JSONObject(payload);
		}
		catch (JSONException e) {
			throw new ContentNotSupportedException("Payload is not well-formed JSON ");
		}

		// todo test payload is rifcs
	}

	/**
	 * Proxy method to find a {@link Request} via the {@link RequestService}
	 * @param id the String id in {@link java.util.UUID}
	 * @return the {@link} Request, null if not found
	 */
	public Request findById(String id) {
		return requestService.findById(id);
	}

	/**
	 * Proxy method to save the {@link Request} via the {@link RequestService}
	 * @param request the {@link Request} to persist
	 * @return the persisted {@link Request}
	 */
	public Request save(Request request) {
		return requestService.save(request);
	}

	/**
	 * Obtain the {@link RequestMapper}
	 * @return {@link RequestMapper}
	 */
	public RequestMapper getRequestMapper() {
		return requestMapper;
	}

	/**
	 * Obtain the {@link RequestService}
	 * @return {@link RequestService}
	 */
	public RequestService getRequestService() {
		return requestService;
	}

}
