package ardc.cerium.igsn.controller;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.igsn.dto.IGSNRecordDTO;
import ardc.cerium.igsn.dto.mapper.IGSNRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/igsn-records")
@ConditionalOnProperty(name = "app.igsn.enabled")
@Tag(name = "IGSN Records Resource API")
public class IGSNRecordsResourceController {

	private final KeycloakService kcService;

	private final RecordService recordService;

	private final IGSNRecordMapper igsnRecordMapper;

	public IGSNRecordsResourceController(KeycloakService kcService, RecordService recordService,
			IGSNRecordMapper igsnRecordMapper) {
		this.kcService = kcService;
		this.recordService = recordService;
		this.igsnRecordMapper = igsnRecordMapper;
	}

	@GetMapping("")
	@Operation(summary = "Get all IGSN Records",
			description = "Retrieves all IGSN records that the current user has access to")
	@PageableOperation
	public ResponseEntity<Page<IGSNRecordDTO>> index(HttpServletRequest request,
			@PageableDefault @Parameter(hidden = true) Pageable pageable,
			@RequestParam(required = false) String title, @RequestParam(required = false) String igsn) {
		RecordSpecification specs = new RecordSpecification();
		User user = kcService.getLoggedInUser(request);

		// build ownerIDs list
		List<UUID> ownerIDs = user.getDataCenters().stream().map(DataCenter::getId).collect(Collectors.toList());
		ownerIDs.add(user.getId());

		// building a search specification, by default ownerID in the provided list
		specs.add(new SearchCriteria("ownerID", ownerIDs, SearchOperation.IN));
		specs.add(new SearchCriteria("type", "IGSN", SearchOperation.EQUAL));

		if (title != null) {
			specs.add(new SearchCriteria("title", title, SearchOperation.MATCH));
		}

		if (igsn != null) {
			specs.add(new SearchCriteria("value", igsn, SearchOperation.IDENTIFIER_EQUAL));
		}

		// perform the search
		Page<Record> result = recordService.search(specs, pageable);

		return ResponseEntity.ok().body(result.map(igsnRecordMapper.getConverter()));
	}

}
