package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.dto.mapper.RequestMapper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.specs.*;
import ardc.cerium.core.common.service.IdentifierService;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import com.google.common.base.Converter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Tag(name = "Request Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RequestResourceController {

	final KeycloakService kcService;

	final RequestService requestService;

	final RecordService recordService;

	final RecordMapper recordMapper;

	final IdentifierService identifierService;

	final IdentifierMapper identifierMapper;

	final RequestMapper requestMapper;

	public RequestResourceController(KeycloakService kcService, RequestService requestService,
			RecordService recordService, RecordMapper recordMapper, IdentifierService identifierService,
			IdentifierMapper identifierMapper, RequestMapper requestMapper) {
		this.kcService = kcService;
		this.requestService = requestService;
		this.recordService = recordService;
		this.recordMapper = recordMapper;
		this.identifierService = identifierService;
		this.identifierMapper = identifierMapper;
		this.requestMapper = requestMapper;
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
		RequestSpecification specs = getSearchSpecification(user, type, status, allocationID);
		Page<Request> result = requestService.search(specs, pageable);
		Page<RequestDTO> dtos = result.map(requestMapper.getConverter());
		return ResponseEntity.ok(dtos);
	}

	public RequestSpecification getSearchSpecification(User user, String type, String status, String allocationID) {
		RequestSpecification specs = new RequestSpecification();

		if (type != null) {
			specs.add(new SearchCriteria("type", type, SearchOperation.EQUAL));
		}

		if (status != null) {
			specs.add(new SearchCriteria("status", Request.Status.valueOf(status), SearchOperation.EQUAL));
		}

		if (allocationID != null) {
			specs.add(new SearchCriteria("allocationID", UUID.fromString(allocationID), SearchOperation.EQUAL));
		}
		else {
			// owned by all the allocations the user have access to
			List<UUID> allocationIDs = user.getAllocations().stream().map(Allocation::getId)
					.collect(Collectors.toList());
			specs.add(new SearchCriteria("allocationID", allocationIDs, SearchOperation.IN));
		}
		return specs;
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<RequestDTO> show(@PathVariable String id, HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping(value = "/{id}/logs")
	public ResponseEntity<String> showLogs(@PathVariable String id, HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		String logPath = requestService.getLoggerPathFor(request);
		File logFile = new File(logPath);
		if (!logFile.exists()) {
			throw new RuntimeException(String.format("Logs Path: %s doesn't exist", logPath));
		}
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

	@GetMapping(value = "/{id}/records")
	@PageableOperation
	public ResponseEntity<Page<RecordDTO>> showRecords(@PathVariable String id, HttpServletRequest httpRequest,
			Pageable pageable) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		RecordSpecification specs = new RecordSpecification();
		specs.add(new SearchCriteria("requestID", request.getId(), SearchOperation.EQUAL));
		Page<Record> result = recordService.search(specs, pageable);
		Page<RecordDTO> resultDTO = result.map(recordMapper.getConverter());

		return ResponseEntity.ok().body(resultDTO);
	}

	@GetMapping(value = "/{id}/identifiers")
	@PageableOperation
	public ResponseEntity<Page<IdentifierDTO>> showIdentifiers(@PathVariable String id, HttpServletRequest httpRequest,
			Pageable pageable) {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);

		IdentifierSpecification specs = new IdentifierSpecification();
		specs.add(new SearchCriteria("requestID", request.getId(), SearchOperation.RECORD_EQUAL));
		Page<Identifier> result = identifierService.search(specs, pageable);
		Page<IdentifierDTO> resultDTO = result.map(identifierMapper.getConverter());
		return ResponseEntity.ok(resultDTO);
	}

	@PostMapping(value = "/")
	public ResponseEntity<RequestDTO> store(@RequestBody RequestDTO requestDTO, HttpServletRequest httpServletRequest) {
		User user = kcService.getLoggedInUser(httpServletRequest);
		Request request = requestService.create(requestDTO, user);

		RequestDTO dto = requestMapper.getConverter().convert(request);
		URI location = URI.create("/api/resources/requests/" + dto.getId().toString());
		return ResponseEntity.created(location).body(dto);
	}

	@PostMapping(value = "{id}/logs")
	public ResponseEntity<String> appendLog(@PathVariable String id, @RequestBody String message,
			HttpServletRequest httpRequest) throws IOException {
		User user = kcService.getLoggedInUser(httpRequest);
		Request request = requestService.findOwnedById(id, user);
		message = Helpers.getLine(message, 0);
		requestService.getLoggerFor(request).info(message);
		requestService.closeLoggerFor(request);

		String logPath = requestService.getLoggerPathFor(request);
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

}
