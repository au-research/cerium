package ardc.cerium.drvs.controller;

import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.drvs.model.AllocationValidationSummary;
import ardc.cerium.drvs.service.DRVSIndexingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(name = "app.drvs.enabled")
@RequestMapping(value = "/api/resources/drvs-summary/", produces = { MediaType.APPLICATION_JSON_VALUE })
@Tag(name = "DRVS Summary Resources", description = "API endpoints to obtain Allocation DRVS Summary")
@SecurityRequirement(name = "basic")
@SecurityRequirement(name = "oauth2")
public class DRVSSummaryController {

	final KeycloakService kcService;

	final DRVSIndexingService drvsIndexingService;

	public DRVSSummaryController(KeycloakService kcService, DRVSIndexingService drvsIndexingService) {
		this.kcService = kcService;
		this.drvsIndexingService = drvsIndexingService;
	}

	@GetMapping(value = "")
	public ResponseEntity<List<AllocationValidationSummary>> index(HttpServletRequest httpRequest) {
		User user = kcService.getLoggedInUser(httpRequest);

		List<UUID> allocationIDs = user.getAllocations().stream().map(Allocation::getId).collect(Collectors.toList());
		List<AllocationValidationSummary> results = new ArrayList<>();

		for (UUID allocationID : allocationIDs) {
			AllocationValidationSummary summary = drvsIndexingService.getSummaryForAllocation(allocationID.toString());
			results.add(summary);
		}

		return ResponseEntity.ok().body(results);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<AllocationValidationSummary> show(@PathVariable String id) {
		AllocationValidationSummary summary = drvsIndexingService.getSummaryForAllocation(id);
		return ResponseEntity.ok().body(summary);
	}

}
