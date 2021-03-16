package ardc.cerium.drvs.controller;

import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.drvs.dto.DRVSRecordDocument;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
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

import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

@RestController
@RequestMapping(value = "/api/search/drvs-records")
@ConditionalOnProperty(name = "app.drvs.enabled")
@Tag(name = "DRVS Record Search API", description = "Search API for DRVS Records")
public class DRVSRecordSearchController {

	final KeycloakService kcService;

	final ElasticsearchOperations elasticsearchOperations;

	public DRVSRecordSearchController(KeycloakService kcService, ElasticsearchOperations elasticsearchOperations) {
		this.kcService = kcService;
		this.elasticsearchOperations = elasticsearchOperations;
	}

	@GetMapping("")
	public ResponseEntity<SearchPage<DRVSRecordDocument>> search(HttpServletRequest httpRequest,
			@Parameter(hidden = true) @PageableDefault(size = 100) Pageable pageable,
			@Parameter(name = "allocationID", description = "The AllocationID filter",
					schema = @Schema(implementation = UUID.class)) @RequestParam(required = false) String allocationID,
			@Parameter(name = "status", description = "Status Filter") @RequestParam(required = false) String status,
			@Parameter(name = "query", description = "Search Query") @RequestParam(required = false) String query) {
		User user = kcService.getLoggedInUser(httpRequest);

		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

		// allocationID
		if (allocationID != null && !allocationID.trim().equals("")) {
			boolQueryBuilder.must(QueryBuilders.matchQuery("allocationID", allocationID));
		}
		else {
			List<String> ownerIDs = user.getDataCenters().stream().map(DataCenter::getId).map(UUID::toString)
					.collect(Collectors.toList());
			ownerIDs.add(user.getId().toString());
			ownerIDs.forEach(id -> boolQueryBuilder.should(QueryBuilders.matchQuery("ownerID", id)));
		}

		// query
		if (query != null && !query.trim().equals("")) {
			boolQueryBuilder.must(QueryBuilders.multiMatchQuery(query, "title", "DOI", "localCollectionID"));
		}

		// status
		if (status != null && !status.trim().isEmpty()) {
			boolQueryBuilder.must(QueryBuilders.termQuery("status", status));
		}

		Query searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder).withPageable(pageable)
				.addAggregation(terms("statuses").field("status")).build();

		SearchHits<DRVSRecordDocument> result = elasticsearchOperations.search(searchQuery, DRVSRecordDocument.class);
		SearchPage<DRVSRecordDocument> resultPage = SearchHitSupport.searchPageFor(result, pageable);

		return ResponseEntity.ok().body(resultPage);
	}

}
