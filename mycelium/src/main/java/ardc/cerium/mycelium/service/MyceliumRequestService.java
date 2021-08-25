package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.exception.ContentNotSupportedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@Slf4j
@Service
public class MyceliumRequestService {

    public static final String IMPORT_REQUEST_TYPE = "mycelium-import";

    public static final String DELETE_REQUEST_TYPE = "mycelium-delete";

    public static final String AFFECTED_REL_REQUEST_TYPE = "mycelium-affected_relationships";

    @Autowired
    RequestMapper requestMapper;

    @Autowired
    RequestService requestService;

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

	public Request saveToPayloadPath(Request request, String payload) {
        String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
        try {
            Files.createFile(Paths.get(payloadPath));
            Files.writeString(Paths.get(payloadPath), payload);
        }
        catch (IOException e) {
            log.error("Failed to write payload [payloadPath={}] [content={}] Reason: {}", payloadPath, payload, e.getMessage());
        }
        return request;
    }

    public void validateRequestDTO(RequestDTO requestDTO) throws Exception {
        String requestType = requestDTO.getType();
        if (!(requestType.equals(IMPORT_REQUEST_TYPE) || requestType.equals(DELETE_REQUEST_TYPE)
                || requestType.equals(AFFECTED_REL_REQUEST_TYPE))) {
            throw new ContentNotSupportedException(String.format("Invalid Request Type: %s", requestType));
        }
    }

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

	public RequestMapper getRequestMapper() {
	    return requestMapper;
    }

    public RequestService getRequestService() {
        return requestService;
    }
}
