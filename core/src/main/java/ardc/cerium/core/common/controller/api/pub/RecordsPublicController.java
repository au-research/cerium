package ardc.cerium.core.common.controller.api.pub;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import ardc.cerium.core.common.service.RecordService;
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
@RequestMapping(value = "/api/public/records", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Record Public API")
public class RecordsPublicController {

	final RecordService service;

	final VersionService versionService;

	final RecordMapper recordMapper;

	final VersionMapper versionMapper;

	public RecordsPublicController(RecordService service, VersionService versionService, RecordMapper recordMapper,
			VersionMapper versionMapper) {
		this.service = service;
		this.versionService = versionService;
		this.recordMapper = recordMapper;
		this.versionMapper = versionMapper;
	}

	@GetMapping("")
	@Operation(summary = "Get all publicly available records", description = "Retrieves all publicly available records")
	@PageableOperation
	public ResponseEntity<Page<RecordDTO>> index(@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String type) {

		RecordSpecification searchSpecification = new RecordSpecification();
		searchSpecification.add(new SearchCriteria("visible", true, SearchOperation.EQUAL));

		if (type != null) {
			searchSpecification.add(new SearchCriteria("type", type, SearchOperation.EQUAL));
		}

		Page<Record> result = service.search(searchSpecification, pageable);
		return ResponseEntity.ok().body(result.map(recordMapper.getConverter()));
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single record by id", description = "Retrieve the metadata for a single record by id")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Record is found",
			content = @Content(schema = @Schema(implementation = RecordDTO.class)))
	public ResponseEntity<RecordDTO> show(@Parameter(required = true, description = "the id of the record (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		Record record = service.findPublicById(id);
		RecordDTO dto = recordMapper.convertToDTO(record);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping(value = "/{id}/versions")
	@Operation(summary = "Get all versions for a record",
			description = "Retrieve the versions for a single record by id")
	@ApiResponse(responseCode = "404", description = "Record is not found",
			content = @Content(schema = @Schema(implementation = APIExceptionResponse.class)))
	@ApiResponse(responseCode = "200", description = "Versions are found",
			content = @Content(schema = @Schema(implementation = Page.class)))
	@PageableOperation
	public ResponseEntity<?> showVersions(
			@Parameter(required = true, description = "the id of the record (uuid)",
					schema = @Schema(implementation = UUID.class)) @PathVariable String id,
			@RequestParam(required = false) String schema, @Parameter(hidden = true) Pageable pageable) {
		// try to reuse the business logic of finding public record
		Record record = service.findPublicById(id);

		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("record", record, SearchOperation.EQUAL));

		if (schema != null) {
			specs.add(new SearchCriteria("schema", schema, SearchOperation.EQUAL));
		}

		Page<Version> result = versionService.search(specs, pageable);
		return ResponseEntity.ok().body(result.map(versionMapper.getConverter()));
	}

}
