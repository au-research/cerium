package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.controller.api.PageableOperation;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.service.MyceliumService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/search/relationships")
@Slf4j
public class RelationshipsAPIController {

	private final MyceliumService myceliumService;

	public RelationshipsAPIController(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
	}

	@GetMapping("")
	@PageableOperation
	public ResponseEntity<?> search(@Parameter(hidden = true) @PageableDefault(size = 100) Pageable pageable,
			@Parameter(name = "fromIdentifierValue",
					description = "From Identifier Value") @RequestParam(required = false) String fromIdentifierValue,
			@Parameter(name = "fromIdentifierType",
					description = "From Identifier Type") @RequestParam(required = false) String fromIdentifierType) {

		// builds the criteriaList
		List<SearchCriteria> criteriaList = new ArrayList<>();
		if (fromIdentifierValue != null) {
			criteriaList.add(new SearchCriteria("fromIdentifierValue", fromIdentifierValue, SearchOperation.EQUAL));
		}
		if (fromIdentifierType != null) {
			criteriaList.add(new SearchCriteria("fromIdentifierType", fromIdentifierType, SearchOperation.EQUAL));
		}

		// obtain the paginated result from myceliumService
		Page<Relationship> result = myceliumService.search(criteriaList, pageable);

		// todo convert result to DTOs for better presentation
		return ResponseEntity.ok(result);
	}

}
