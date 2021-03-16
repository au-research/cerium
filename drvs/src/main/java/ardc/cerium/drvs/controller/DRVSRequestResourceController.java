package ardc.cerium.drvs.controller;

import ardc.cerium.drvs.dto.mapper.DRVSRequestMapper;
import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.controller.api.resources.RequestResourceController;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.specs.RequestSpecification;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.drvs.dto.DRVSRequestDTO;
import ardc.cerium.core.exception.ForbiddenOperationException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/drvs-requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "DRVS Request Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class DRVSRequestResourceController extends RequestResourceController {

	private static final Logger logger = LoggerFactory.getLogger(DRVSRequestResourceController.class);

	final DRVSRequestMapper drvsRequestMapper;

	final RequestService requestService;

	final KeycloakService kcService;

	public DRVSRequestResourceController(KeycloakService kcService, RequestService requestService,
			RecordService recordService, RecordMapper recordMapper, IdentifierService identifierService,
			IdentifierMapper identifierMapper, RequestMapper requestMapper, DRVSRequestMapper drvsRequestMapper) {
		super(kcService, requestService, recordService, recordMapper, identifierService, identifierMapper,
				requestMapper);
		this.requestService = requestService;
		this.kcService = kcService;
		this.drvsRequestMapper = drvsRequestMapper;
	}

	@GetMapping(value = "/")
	@PageableOperation
	public ResponseEntity<Page<RequestDTO>> index(HttpServletRequest httpServletRequest,
			@Parameter(hidden = true) @PageableDefault(size = 20, sort = "updatedAt",
					direction = Sort.Direction.DESC) Pageable pageable,
			@Parameter(name = "allocationID", description = "The AllocationID filter",
					schema = @Schema(implementation = UUID.class)) @RequestParam(required = false) String allocationID,
			@Parameter(name = "type",
					description = "The Request type filter") @RequestParam(required = false) String type,
			@Parameter(name = "status",
					description = "The Status filter") @RequestParam(required = false) String status) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		RequestSpecification specs = super.getSearchSpecification(user, type, status, allocationID);
		Page<Request> result = requestService.search(specs, pageable);
		Page<RequestDTO> dtos = result.map(drvsRequestMapper.getConverter());
		return ResponseEntity.ok().body(dtos);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<RequestDTO> show(@PathVariable String id, HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findById(id);

		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Request with id %s doesn't exist", id));
		}

		// check owner by AllocationID instead
		List<UUID> allocationIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		if (!allocationIDs.contains(request.getAllocationID())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		DRVSRequestDTO dto = drvsRequestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping(value = "/{id}/logs")
	public ResponseEntity<String> showLogs(@PathVariable String id, HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findById(id);
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Request with id %s doesn't exist", id));
		}
		// check owner by AllocationID instead
		List<UUID> allocationIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		if (!allocationIDs.contains(request.getAllocationID())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		String logPath = requestService.getLoggerPathFor(request);
		File logFile = new File(logPath);
		if (!logFile.exists()) {
			throw new RuntimeException(String.format("Logs Path: %s doesn't exist", logPath));
		}
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

	@GetMapping(value = "/{id}/payload")
	public ResponseEntity<ByteArrayResource> download(@PathVariable String id, HttpServletRequest httpRequest) {
		logger.info("Download request for RequestID: {}", id);

		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findById(id);
		if (request == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Request with id %s doesn't exist", id));
		}
		// check owner by AllocationID instead
		List<UUID> allocationIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		if (!allocationIDs.contains(request.getAllocationID())) {
			throw new ForbiddenOperationException("User does not have access to this request");
		}

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		ByteArrayResource resource;
		String fileName = String.format("%s_%s.csv", request.getId(), request.getCreatedAt());
		MediaType mediaType = MediaType.TEXT_PLAIN;

		try {
			logger.info("Loading content for file at {}", payloadPath);
			byte[] bytes = FileUtils.readFileToByteArray(new File(payloadPath));
			resource = new ByteArrayResource(bytes);
		}
		catch (IOException e) {
			logger.error("Failed to read file at {}, Reason: {}", payloadPath, e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
		}

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
				.contentType(mediaType).contentLength(resource.getByteArray().length).body(resource);
	}

}
