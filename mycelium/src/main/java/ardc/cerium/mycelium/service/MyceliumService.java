package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.task.DeleteTask;
import ardc.cerium.mycelium.task.ImportTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
@Getter
public class MyceliumService {

	private final GraphService graphService;

	private final MyceliumRequestService myceliumRequestService;

	private final MyceliumIndexingService indexingService;

	private final MyceliumSideEffectService myceliumSideEffectService;

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

	public void runDeleteTask(Request request) {
		DeleteTask deleteTask = new DeleteTask(request, this);
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

	public void deleteRecord(String recordId) throws Exception {
		Vertex vertex = graphService.getVertexByIdentifier(recordId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (vertex == null) {
			throw new Exception(String.format("Record with ID %s doesn't exist", recordId));
		}
		graphService.deleteVertex(vertex);
		indexingService.deleteRelationship(vertex.getIdentifier());
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

}
