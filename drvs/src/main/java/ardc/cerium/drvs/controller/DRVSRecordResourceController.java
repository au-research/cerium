package ardc.cerium.drvs.controller;

import ardc.cerium.drvs.dto.mapper.DRVSRecordMapper;
import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.drvs.dto.DRVSRecordDTO;
import ardc.cerium.drvs.service.DRVSImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/drvs-records")
@ConditionalOnProperty(name = "app.drvs.enabled")
@Tag(name = "DRVS Record Resources API", description = "REST API for DRVS Records")
public class DRVSRecordResourceController {

	private final RecordService recordService;

	private final KeycloakService kcService;

	private final DRVSRecordMapper drvsRecordMapper;

	public DRVSRecordResourceController(RecordService recordService, KeycloakService kcService,
			DRVSRecordMapper drvsRecordMapper) {
		this.recordService = recordService;
		this.kcService = kcService;
		this.drvsRecordMapper = drvsRecordMapper;
	}

	@GetMapping("")
	@Operation(summary = "Get all DRVS Records", description = "Retrieves all identifier resources")
	@PageableOperation
	public ResponseEntity<Page<DRVSRecordDTO>> index(HttpServletRequest request,
			@Parameter(hidden = true) @PageableDefault(size = 100, sort = "modifiedAt",
					direction = Sort.Direction.DESC) Pageable pageable,
			@Parameter(name = "allocationID", description = "The AllocationID filter",
					schema = @Schema(implementation = UUID.class)) @RequestParam(required = false) String allocationID,
			@Parameter(name = "title", description = "Filter by title") @RequestParam(required = false) String title) {

		User user = kcService.getLoggedInUser(request);
		RecordSpecification specs = new RecordSpecification();

		// only search for DRVSRecord
		specs.add(new SearchCriteria("type", DRVSImportService.DRVS_RECORD_TYPE, SearchOperation.EQUAL));

		if (allocationID != null) {
			// translate allocationID to DataCenterID and then use that as ownerID
			String ownerID;
			try {
				Allocation allocation = kcService.getAllocationByResourceID(allocationID);
				DataCenter dataCenter = kcService.getDataCenterByUUID(allocation.getDataCenters().get(0).getId());
				ownerID = dataCenter.getId().toString();
			}
			catch (Exception e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						String.format("Failed to obtain DataCenter for Allocation %s", allocationID));
			}
			specs.add(new SearchCriteria("ownerID", UUID.fromString(ownerID), SearchOperation.EQUAL));
		}
		else {
			// all owner
			List<UUID> ownerIDs = user.getDataCenters().stream().map(DataCenter::getId).collect(Collectors.toList());
			ownerIDs.add(user.getId());
			specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.IN));
		}

		if (title != null) {
			specs.add(new SearchCriteria("title", title, SearchOperation.MATCH));
		}

		// perform the search
		Page<Record> result = recordService.search(specs, pageable);
		return ResponseEntity.ok().body(result.map(drvsRecordMapper.getConverter()));
	}

	@GetMapping(value = "/{id}")
	@Operation(summary = "Get a single DRVS Record by Id")
	public ResponseEntity<DRVSRecordDTO> show(@Parameter(required = true, description = "the id of the record (uuid)",
			schema = @Schema(implementation = UUID.class)) @PathVariable String id) {
		Record record = recordService.findById(id);
		DRVSRecordDTO dto = drvsRecordMapper.getConverter().convert(record);
		return ResponseEntity.ok().body(dto);
	}

}
