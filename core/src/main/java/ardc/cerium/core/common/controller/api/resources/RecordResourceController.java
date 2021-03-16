package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.APIExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/records")
@Tag(name = "Record Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class RecordResourceController {

	private final RecordService recordService;

	private final KeycloakService kcService;

	private final VersionService versionService;

	private final RecordMapper recordMapper;

	private final VersionMapper versionMapper;

	public RecordResourceController(RecordService recordService, KeycloakService kcService,
			VersionService versionService, RecordMapper recordMapper, VersionMapper versionMapper) {
		this.recordService = recordService;
		this.kcService = kcService;
		this.versionService = versionService;
		this.recordMapper = recordMapper;
		this.versionMapper = versionMapper;
	}

	@GetMapping("")
	@Operation(summary = "Get all records",
			description = "Retrieves all record resources that the current user has access to")
	@PageableOperation
	public ResponseEntity<Page<RecordDTO>> index(HttpServletRequest request,
			@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String title) {
		// obtain a list of ownerIDs include the current user ownerID
		User user = kcService.getLoggedInUser(request);
		List<UUID> ownerIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		ownerIDs.add(user.getId());

		// building a search specification, by default ownerID in the provided list
		RecordSpecification specs = new RecordSpecification();
		specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.IN));
		if (title != null) {
			specs.add(new SearchCriteria("title", title, SearchOperation.MATCH));
		}

		// perform the search
		Page<Record> result = recordService.search(specs, pageable);

		return ResponseEntity.ok().body(result.map(recordMapper.getConverter()));
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single record by id", description = "Retrieve the metadata for a single record by id")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Record is found",
			content = @Content(schema = @Schema(implementation = RecordDTO.class)))
	@ApiResponse(responseCode = "403", description = "User doesn't have access to the record",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<RecordDTO> show(
			@Parameter(required = true, description = "the id of the record (uuid)",
					schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		Record record = recordService.findOwnedById(id, user);
		RecordDTO dto = recordMapper.convertToDTO(record);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping("/")
	@Operation(summary = "Creates a new record", description = "Add a new record to the registry")
	@ApiResponse(responseCode = "201", description = "Record is created",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "403", description = "Operation is forbidden",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<RecordDTO> store(@RequestBody RecordDTO recordDTO, HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		Record record = recordService.create(recordDTO, user);
		RecordDTO dto = recordMapper.convertToDTO(record);
		return ResponseEntity.created(URI.create("/api/resources/records/" + dto.getId())).body(dto);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a record by ID", description = "Update an existing record")
	@ApiResponse(responseCode = "202", description = "Record is updated",
			content = @Content(schema = @Schema(implementation = Record.class)))
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "403", description = "User doesn't have access to the record",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<RecordDTO> update(
			@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			@RequestBody RecordDTO recordDTO, HttpServletRequest request) {
		recordDTO.setId(UUID.fromString(id));
		User user = kcService.getLoggedInUser(request);
		Record record = recordService.update(recordDTO, user);
		RecordDTO dto = recordMapper.convertToDTO(record);
		return ResponseEntity.accepted().body(dto);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a record by ID", description = "Delete a record from the registry")
	@ApiResponse(responseCode = "202", description = "Record is deleted")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "404", description = "User doesn't have access to the record",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<?> destroy(@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		recordService.delete(id, user);
		return ResponseEntity.accepted().body(null);
	}

	@GetMapping(value = "/{id}/versions")
	@Operation(summary = "Get versions by Record ID", description = "Show all versions for a given record")
	@PageableOperation
	public ResponseEntity<Page<VersionDTO>> showVersions(HttpServletRequest request, Pageable pageable,
			@PathVariable String id, @RequestParam(required = false) String schema) {
		User user = kcService.getLoggedInUser(request);
		Record record = recordService.findOwnedById(id, user);
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("record", record, SearchOperation.EQUAL));
		if (schema != null) {
			specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		}
		Page<Version> result = versionService.search(specs, pageable);
		return ResponseEntity.ok().body(result.map(versionMapper.getConverter()));
	}

}