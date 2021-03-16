package ardc.cerium.core.common.controller.api.pub;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.schema.JSONSchema;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.exception.APIExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/public/versions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Versions Public API")
public class VersionsPublicController {

	final VersionService versionService;

	final RecordService recordService;

	final VersionMapper versionMapper;

	final SchemaService schemaService;

	public VersionsPublicController(VersionService versionService, RecordService recordService,
			VersionMapper versionMapper, SchemaService schemaService) {
		this.versionService = versionService;
		this.recordService = recordService;
		this.versionMapper = versionMapper;
		this.schemaService = schemaService;
	}

	@GetMapping("")
	@Operation(summary = "Get all publicly available versions",
			description = "Return versions from publicly available records")
	@PageableOperation
	public ResponseEntity<Page<VersionDTO>> index(@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String schema, @RequestParam(required = false) String record) {
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		specs.add(new SearchCriteria("current", true, SearchOperation.EQUAL));

		if (schema != null) {
			specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		}

		if (record != null) {
			Record recordEntity = recordService.findById(record);
			specs.add(new SearchCriteria("record", recordEntity, SearchOperation.EQUAL));
		}

		Page<Version> result = versionService.search(specs, pageable);
		Page<VersionDTO> resultDTO = result.map(versionMapper.getConverter());

		return ResponseEntity.ok().body(resultDTO);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a single public version by id",
			description = "Retrieve the metadata for a single version by id")
	@ApiResponse(responseCode = "404", description = "Version is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Version is found",
			content = @Content(schema = @Schema(implementation = VersionDTO.class)))
	public ResponseEntity<VersionDTO> show(@Parameter(required = true, description = "the id of the version (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		Version version = versionService.findPublicById(id);
		VersionDTO dto = versionMapper.convertToDTO(version);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping("/{id}/content")
	@Operation(summary = "Display the content of a public version",
			description = "Return the content of the public version")
	@ApiResponse(responseCode = "404", description = "Version is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	public ResponseEntity<?> showContent(@Parameter(required = true, description = "the id of the version (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		Version version = versionService.findPublicById(id);

		MediaType mediaType = MediaType.APPLICATION_XML;
		if (schemaService.getSchemaByID(version.getSchema()).getClass().equals(JSONSchema.class)) {
			mediaType = MediaType.APPLICATION_JSON;
		}

		return ResponseEntity.ok().contentType(mediaType).body(version.getContent());
	}

}
