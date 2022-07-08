package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.model.*;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.PrimaryKeySetting;
import ardc.cerium.mycelium.rifcs.model.datasource.settings.primarykey.PrimaryKey;
import ardc.cerium.mycelium.task.DeleteTask;
import ardc.cerium.mycelium.task.ImportTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@Getter
public class MyceliumService {

	private final GraphService graphService;

	private final MyceliumRequestService myceliumRequestService;

	private final MyceliumIndexingService indexingService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	private final MyceliumIndexingService myceliumIndexingService;

	private final ApplicationEventPublisher applicationEventPublisher;

	@PostConstruct
	public void init() {

		// avoid circular dependency with post-construct injection
		myceliumSideEffectService.setMyceliumService(this);
	}

	public RegistryObject parsePayloadToRegistryObject(String payload) throws JsonProcessingException {
		return RIFCSGraphProvider.parsePayloadToRegistryObject(payload);
	}

	public RecordState getRecordState(String registryObjectId) {
		return graphService.getRecordState(registryObjectId);
	}

	public Request createRequest(RequestDTO requestDTO) {
		return myceliumRequestService.createRequest(requestDTO);
	}

	public void saveToPayloadPath(Request request, String payload) {
		myceliumRequestService.saveToPayloadPath(request, payload);
	}

	public Request save(Request request) {
		return myceliumRequestService.save(request);
	}

	public Request findById(String id) {
		return myceliumRequestService.findById(id);
	}

	public void validateRequest(Request request) {
		if (request.getType().equals(MyceliumRequestService.IMPORT_REQUEST_TYPE)) {
			myceliumRequestService.validateImportRequest(request);
		}
	}

	public void runImportTask(Request request) {
		ImportTask importTask = new ImportTask(request, this);
		importTask.run();
	}

	public void runImportTask(String json, Request request) {
		ImportTask importTask = new ImportTask(json, request, this);
		importTask.run();
	}

	public void runDeleteTask(Request request) {
		DeleteTask deleteTask = new DeleteTask(request, this);
		deleteTask.run();
	}

	public void runDeleteTask(String registryObjectId, Request request) {
		DeleteTask deleteTask = new DeleteTask(registryObjectId, request, this);
		deleteTask.run();
	}

	public void ingestRegistryObject(RegistryObject registryObject) {
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();
		Graph graph = graphProvider.get(registryObject);

		// delete the original vertex with detach before insertion
		Vertex original = getVertexFromRegistryObjectId(registryObject.getRegistryObjectId().toString());
		if (original != null) {
			graphService.deleteVertex(original);
		}

		graphService.ingestGraph(graph);
	}

	public Vertex getVertexFromRegistryObjectId(String registryObjectId) {
		return graphService.getVertexByIdentifier(registryObjectId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
	}

	public Vertex getRegistryObjectVertexFromKey(String key) {
		return graphService.getRegistryObjectByKey(key);
	}

	public Vertex getIdentifierVertex(String identifierValue, String identifierType) {
		return graphService.getVertexByIdentifier(identifierValue, identifierType);
	}

	public void deleteRecord(String recordId) throws Exception {
		Vertex vertex = graphService.getVertexByIdentifier(recordId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (vertex == null) {
			log.error("Record with ID {} doesn't exist", recordId);
			return;
		}
		graphService.deleteVertex(vertex);

		// delete all the relationships for that recordId
		indexingService.deleteRelationship(recordId);
	}

	public void indexVertex(Vertex vertex) {
		indexingService.indexVertex(vertex);
	}

	/**
	 * Search for relationships.
	 * @param criteriaList a list of {@link SearchCriteria} to start the search in
	 * @param pageable the pagination and sorting provided by {@link Pageable}
	 * @return a {@link PageImpl} of {@link Relationship}
	 */
	public Page<Relationship> search(List<SearchCriteria> criteriaList, Pageable pageable) {
		List<Relationship> result = new ArrayList<>(graphService.search(criteriaList, pageable));
		int total = graphService.searchCount(criteriaList);
		return new PageImpl<>(result, pageable, total);
	}

	/**
	 * Import a DataSource into the Graph
	 * @param dto the {@link DataSource} deserialized
	 */
	public void importDataSource(DataSource dto) {
		Vertex dataSourceVertex = graphService.getVertexByIdentifier(dto.getId(),
				RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);

		// dataSourceVertex doesn't exist, create it
		if (dataSourceVertex == null) {
			log.debug("Creating DataSource[id={}]", dto.getId());
			dataSourceVertex = new Vertex(dto.getId(), RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
			dataSourceVertex.addLabel(Vertex.Label.DataSource);
			dataSourceVertex.setTitle(dto.getTitle());
			graphService.ingestVertex(dataSourceVertex);
		}

		// update the edges according to primaryKeySettings from dto
		PrimaryKeySetting primaryKeySetting = dto.getPrimaryKeySetting();
		if (primaryKeySetting.isEnabled()) {
			Graph graph = new Graph();
			graph.addVertex(dataSourceVertex);
			Vertex finalDataSourceVertex1 = dataSourceVertex;
			primaryKeySetting.getPrimaryKeys().forEach(primaryKey -> {
				// the edge to add in will be in the format of $class_$relationType (e.g.
				// collection_isFundedBy)
				Vertex primaryKeyVertex = new Vertex(primaryKey.getKey(), RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
				graph.addVertex(primaryKeyVertex);
				if (primaryKey.getRelationTypeFromCollection() != null) {
					graph.addEdge(new Edge(finalDataSourceVertex1, primaryKeyVertex,
							"collection_" + primaryKey.getRelationTypeFromCollection()));
				}
				if (primaryKey.getRelationTypeFromParty() != null) {
					graph.addEdge(new Edge(finalDataSourceVertex1, primaryKeyVertex,
							"party_" + primaryKey.getRelationTypeFromParty()));
				}
				if (primaryKey.getRelationTypeFromActivity() != null) {
					graph.addEdge(new Edge(finalDataSourceVertex1, primaryKeyVertex,
							"activity_" + primaryKey.getRelationTypeFromActivity()));
				}
				if (primaryKey.getRelationTypeFromService() != null) {
					graph.addEdge(new Edge(finalDataSourceVertex1, primaryKeyVertex,
							"service_" + primaryKey.getRelationTypeFromService()));
				}
			});
			graphService.ingestGraph(graph);
		}
	}

	/**
	 * Get all DataSource
	 * @return a {@link List} of {@link DataSource}
	 */
	public List<DataSource> getDataSources() {
		Collection<String> dataSourceIds = graphService
				.getVertexIdentifiersByType(RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
		return dataSourceIds.stream().map(this::getDataSourceById).collect(Collectors.toList());
	}

	/**
	 * Obtain a {@link DataSource} instance
	 * @param dataSourceId the data source id
	 * @return {@link DataSource} populated with properties
	 */
	public DataSource getDataSourceById(String dataSourceId) {

		// try to obtain a vertex
		Vertex dataSourceVertex = graphService.getVertexByIdentifier(dataSourceId,
				RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
		if (dataSourceVertex == null) {
			return null;
		}

		// convert the obtained Vertex to a DataSource instance, could be done as a Mapper
		// instead
		DataSource dataSource = new DataSource();
		dataSource.setId(dataSourceVertex.getIdentifier());
		dataSource.setTitle(dataSourceVertex.getTitle());

		// convert outbound relationships from the Vertex to PrimaryKeySetting
		PrimaryKeySetting primaryKeySetting = new PrimaryKeySetting();
		Collection<Relationship> primaryKeySettings = graphService
				.getDirectOutboundRelationships(dataSourceVertex.getIdentifier(), dataSourceVertex.getIdentifierType());
		if (primaryKeySettings.size() == 0) {
			primaryKeySetting.setEnabled(false);
		}
		else {
			primaryKeySettings.forEach(relationship -> {
				// no need to loop through primaryKey since the
				// graphService.getDirectOutboundRelationships
				// would group them into Relationship
				// (DataSource)-[Relationship*edge]->(pk)
				String toKey = relationship.getTo().getIdentifier();
				PrimaryKey primaryKey = primaryKeySetting.getPrimaryKeys().stream()
						.filter(setting -> setting.getKey().equals(toKey)).findFirst().orElse(null);
				if (primaryKey == null) {
					PrimaryKey pk = new PrimaryKey();
					pk.setKey(toKey);
					relationship.getRelations().forEach(relation -> {
						// relation is in $class_$type format (e.g. collection_isFundedBy)
						String relationType = relation.getType();
						String[] bits = relationType.split("_");
						switch (bits[0]) {
						case "collection":
							pk.setRelationTypeFromCollection(bits[1]);
							break;
						case "party":
							pk.setRelationTypeFromParty(bits[1]);
							break;
						case "service":
							pk.setRelationTypeFromService(bits[1]);
							break;
						case "activity":
							pk.setRelationTypeFromActivity(bits[1]);
							break;
						}
					});
					primaryKeySetting.getPrimaryKeys().add(pk);
				}
			});
		}

		dataSource.setPrimaryKeySetting(primaryKeySetting);
		return dataSource;
	}

	/**
	 * Remove the DataSource from the Graph
	 * @param dataSourceId the data source id
	 */
	public void deleteDataSourceById(String dataSourceId) {
		Vertex dataSourceVertex = graphService.getVertexByIdentifier(dataSourceId,
				RIFCSGraphProvider.DATASOURCE_ID_IDENTIFIER_TYPE);
		graphService.deleteVertex(dataSourceVertex);
	}

	public Page<Vertex> getVerticesByDataSource(DataSource dataSource, Pageable pageable) {
		return graphService.getVertexRepository().getVertexByDataSourceId(dataSource.getId(), pageable);
	}

	public void deleteVerticesByDataSource(DataSource dataSource) {
		graphService.getVertexRepository().deleteByDataSourceId(dataSource.getId());
	}

	public void publishEvent(ApplicationEvent event) {
		log.debug("Publishing Event {}", event);
		applicationEventPublisher.publishEvent(event);
	}

}
