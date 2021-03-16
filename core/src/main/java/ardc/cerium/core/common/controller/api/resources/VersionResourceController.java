package ardc.cerium.core.common.controller.api.resources;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.model.schema.JSONSchema;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.APIExceptionResponse;
import ardc.cerium.core.exception.VersionNotFoundException;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Version Resource API")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class VersionResourceController {

	private final VersionMapper versionMapper;

	private final VersionService versionService;

	private final KeycloakService kcService;

	private final SchemaService schemaService;

	public VersionResourceController(VersionMapper versionMapper, VersionService versionService,
			KeycloakService kcService, SchemaService schemaService) {
		this.versionMapper = versionMapper;
		this.versionService = versionService;
		this.kcService = kcService;
		this.schemaService = schemaService;
	}

	@GetMapping("")
	@Operation(summary = "Get all versions",
			description = "Retrieves all versions resources that the current user has access to")
	@PageableOperation
	public ResponseEntity<Page<VersionDTO>> index(HttpServletRequest request,
			@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String schema) {
		// obtain a list of ownerIDs include the current user ownerID
		User user = kcService.getLoggedInUser(request);
		List<UUID> ownerIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		ownerIDs.add(user.getId());

		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.RECORD_IN));
		if (schema != null) {
			specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		}

		Page<Version> result = versionService.search(specs, pageable);
		Page<VersionDTO> resultDTO = result.map(versionMapper.getConverter());

		return ResponseEntity.ok(resultDTO);
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single version by id", description = "Retrieve the metadata for a single version by id")
	@ApiResponse(responseCode = "404", description = "Version is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Version is found",
			content = @Content(schema = @Schema(implementation = Version.class)))
	public ResponseEntity<VersionDTO> show(@Parameter(required = true, description = "the id of the version (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		Version version = versionService.findById(id);
		if (version == null) {
			throw new VersionNotFoundException(id);
		}
		VersionDTO versionDTO = versionMapper.convertToDTO(version);
		return ResponseEntity.ok().body(versionDTO);
	}

	@PostMapping("/")
	@Operation(summary = "Creates a new version", description = "Add a new version to the registry")
	@ApiResponse(responseCode = "201", description = "Version is created",
			content = @Content(schema = @Schema(implementation = VersionDTO.class)))
	public ResponseEntity<VersionDTO> store(@RequestBody VersionDTO versionDTO, HttpServletRequest request) {
		User user = kcService.getLoggedInUser(request);
		Version version = versionService.create(versionDTO, user);
		VersionDTO dto = versionMapper.convertToDTO(version);

		URI location = URI.create("/api/resources/versions/" + dto.getId());
		return ResponseEntity.created(location).body(dto);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a version by ID", description = "Delete a version from the registry")
	@ApiResponse(responseCode = "202", description = "Version is deleted")
	@ApiResponse(responseCode = "404", description = "Version is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<?> destroy(HttpServletRequest request,
			@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		if (!versionService.exists(id)) {
			throw new VersionNotFoundException(id);
		}
		User user = kcService.getLoggedInUser(request);

		Version version = versionService.findById(id);

		// upon deleting a version, end the version instead
		version = versionService.end(version, user);

		VersionDTO dto = versionMapper.convertToDTO(version);
		return ResponseEntity.accepted().body(dto);
	}

	@GetMapping(value = "/{id}/content",
			produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@Operation(summary = "Get a version content", description = "Get a version content by ID")
	@ApiResponse(responseCode = "200", description = "Version content is found and delivered")
	@ApiResponse(responseCode = "404", description = "Version is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<?> content(
			@Parameter(schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		if (!versionService.exists(id)) {
			throw new VersionNotFoundException(id);
		}
		Version version = versionService.findById(id);

		MediaType mediaType = MediaType.APPLICATION_XML;
		if (schemaService.getSchemaByID(version.getSchema()).getClass().equals(JSONSchema.class)) {
			mediaType = MediaType.APPLICATION_JSON;
		}

		return ResponseEntity.ok().contentType(mediaType).body(version.getContent());
	}

}
