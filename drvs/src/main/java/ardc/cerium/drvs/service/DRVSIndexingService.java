package ardc.cerium.drvs.service;

import ardc.cerium.drvs.dto.DRVSRecordDocument;
import ardc.cerium.drvs.dto.mapper.DRVSRecordDocumentMapper;
import ardc.cerium.drvs.repository.DRVSRecordElasticRepository;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.drvs.model.AllocationValidationSummary;
import ardc.cerium.drvs.task.IndexDRVSRecordTask;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class DRVSIndexingService {

	private static final Logger logger = LoggerFactory.getLogger(DRVSIndexingService.class);

	final DRVSRecordDocumentMapper drvsRecordDocumentMapper;

	final RecordService recordService;

	final DRVSRecordElasticRepository elasticsearchRepository;

	final ElasticsearchOperations elasticsearchOperations;

	private ThreadPoolExecutor indexQueue;

	public DRVSIndexingService(DRVSRecordDocumentMapper drvsRecordDocumentMapper, RecordService recordService,
			DRVSRecordElasticRepository elasticsearchRepository, ElasticsearchOperations elasticsearchOperations) {
		this.drvsRecordDocumentMapper = drvsRecordDocumentMapper;
		this.recordService = recordService;
		this.elasticsearchRepository = elasticsearchRepository;
		this.elasticsearchOperations = elasticsearchOperations;
	}

	@PostConstruct
	public void init() {
		indexQueue = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
	}

	/**
	 * Returns the Queue that contains the index record tasks
	 * @return the {@link ThreadPoolExecutor} that holds the IndexQueue
	 */
	public ThreadPoolExecutor getIndexQueue() {
		return indexQueue;
	}

	/**
	 * Queue a record for indexing
	 * @param recordID the UUID in String of the record to index
	 */
	public void queueRecordById(UUID recordID) {
		logger.info("Queueing record recordID: {}", recordID);
		indexQueue.execute(new IndexDRVSRecordTask(recordID, this, recordService));
	}

	/**
	 * Index the record by first converting it to a {@link DRVSRecordDocument} and use
	 * {@link DRVSRecordElasticRepository#save(Object)} to index the {@link Record} to an
	 * ElasticSearch server
	 * @param record the {@link Record} to index
	 */
	public void index(Record record) {
		logger.debug("Indexing record: {}", record.getId());
		DRVSRecordDocument doc = drvsRecordDocumentMapper.getConverter().convert(record);
		if (doc == null) {
			logger.error("Failed to create DRVSRecordDocument for record: {}", record.getId());
			return;
		}
		elasticsearchRepository.save(doc);
		logger.info("Indexed record {}", record.getId());
	}

	/**
	 * Creates the Index and the Mapping if the ElasticSearch server does not have this
	 * yet. The index is automatically created upon starting up the server
	 */
	public void ensureIndexExist() {
		IndexOperations indexOperations = elasticsearchOperations.indexOps(DRVSRecordDocument.class);
		if (indexOperations.exists()) {
			return;
		}
		logger.info("Index for {} does not exist, creating...", DRVSRecordDocument.class);
		indexOperations.create();
		Document mapping = indexOperations.createMapping(DRVSRecordDocument.class);
		indexOperations.putMapping(mapping);
	}

	/**
	 * Obtain the {@link AllocationValidationSummary} for a given Allocation using a combination of searches and aggregations
	 * @param id the UUID in String of the {@link ardc.cerium.core.common.model.Allocation}
	 * @return {@link AllocationValidationSummary}
	 */
	public AllocationValidationSummary getSummaryForAllocation(String id) {
		AllocationValidationSummary summary = new AllocationValidationSummary();
		summary.setAllocatonID(id);

		// obtain relevant counts from aggregations
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
		boolQueryBuilder.must(QueryBuilders.matchQuery("allocationID", id));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
				.addAggregation(AggregationBuilders.terms("statuses").field("status"))
				.addAggregation(AggregationBuilders.count("DOI").field("DOI.raw"));
		Query searchQuery = queryBuilder.build();
		SearchHits<DRVSRecordDocument> result = elasticsearchOperations.search(searchQuery, DRVSRecordDocument.class);

		Terms statuses = result.getAggregations().get("statuses");
		ValueCount doi = result.getAggregations().get("DOI");

		Map<String, Object> statusFacet = statuses.getBuckets().stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), MultiBucketsAggregation.Bucket::getDocCount));

		// populate summary with counts
		summary.setTotal((int) result.getTotalHits());
		summary.setFailed(statusFacet.containsKey("FAILED") ? ((Long) statusFacet.get("FAILED")).intValue() : 0);
		summary.setUnvalidated(
				statusFacet.containsKey("UNVALIDATED") ? ((Long) statusFacet.get("UNVALIDATED")).intValue() : 0);
		summary.setPassed(statusFacet.containsKey("PASSED") ? ((Long) statusFacet.get("PASSED")).intValue() : 0);
		summary.setDoiNotFound(
				statusFacet.containsKey("DOINOTFOUND") ? ((Long) statusFacet.get("DOINOTFOUND")).intValue() : 0);
		summary.setDoiCount(((Long) doi.getValue()).intValue());

		// update specific rules count, ignore records that are not found or unvalidated
		NativeSearchQueryBuilder ruleSearchBuilder = new NativeSearchQueryBuilder()
				.withQuery(new BoolQueryBuilder().must(QueryBuilders.matchQuery("allocationID", id))
						.mustNot(QueryBuilders.matchQuery("status", "DOINOTFOUND"))
						.mustNot(QueryBuilders.matchQuery("status", "Unvalidated")));
		summary.getRules().forEach((key, value) -> ruleSearchBuilder
				.addAggregation(AggregationBuilders.terms(key).field(String.format("validation.rules.%s", key))));
		Query ruleSearchQuery = ruleSearchBuilder.build();
		SearchHits<DRVSRecordDocument> ruleQueryResult = elasticsearchOperations.search(ruleSearchQuery,
				DRVSRecordDocument.class);

		// extract rule facets
		Map<String, Integer> ruleFacets = new HashMap<>();
		summary.getRules().forEach((key, value) -> {
			Terms keyTerms = ruleQueryResult.getAggregations().get(key);

			// only look at the bucket with the value "false"
			// obtain the docCount of the "false" value and store it for usage in ruleFacets
			keyTerms.getBuckets().stream().filter(e -> e.getKeyAsString().equals("false"))
					.findFirst().ifPresent(trueBucket -> ruleFacets.put(key, ((Long) trueBucket.getDocCount()).intValue()));
		});

		// update rule count based on ruleFacets
		for (Map.Entry<String, Integer> entry : ruleFacets.entrySet()) {
			summary.setRuleCount(entry.getKey(), entry.getValue());
		}

		return summary;
	}

}
