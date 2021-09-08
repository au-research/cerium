package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumService {

	private final GraphService graphService;

	public MyceliumService(GraphService graphService) {
		this.graphService = graphService;
	}

	public RegistryObject parsePayloadToRegistryObject(String payload) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(payload, RegistryObject.class);
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

	public void deleteRecord(String recordId) throws Exception {

		Vertex vertex = graphService.getVertexByIdentifier(recordId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (vertex == null) {
			throw new Exception(String.format("Record with ID %s doesn't exist", recordId));
		}
		graphService.deleteVertex(vertex);
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
	 * Finds a {@link Collection} of {@link Vertex} of RegistryObject that is considered
	 * identical. Identical Registry object shares the same Identifier (isSameAs to the
	 * same Identifier). This property is transitive
	 * @param origin the {@link Vertex} to start the search in
	 * @return a {@link Collection} of {@link Vertex} that contains all the identical
	 * {@link Vertex}
	 */
	public Collection<Vertex> getDuplicateRegistryObject(Vertex origin) {
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());

		// only return the RegistryObject
		return sameAsNodeCluster.stream().filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject))
				.collect(Collectors.toList());
	}

	public Collection<Relationship> getAllDirectFrom(Vertex origin, Pageable pageable) {
		return graphService.getMyDuplicateRelationships(origin.getIdentifier(), origin.getIdentifierType(), pageable);
	}

	public Vertex getVertexFromRegistryObjectId(String registryObjectId) {
		return graphService.getVertexByIdentifier(registryObjectId, RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
	}

	/**
	 * Obtain a {@link RecordState} of a RegistryObject given the registryObjectId from
	 * the Graph database as of this moment
	 * @param registryObjectId the registryObjectId identifier
	 * @return the {@link RecordState} or null if the RegistryObject is not present in the
	 * database
	 */
	public RecordState getRecordState(String registryObjectId) {

		// if the registryObjectId doesn't exist in the graph
		Vertex origin = graphService.getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (origin == null) {
			return null;
		}

		RecordState state = new RecordState();
		state.setRegistryObjectId(registryObjectId);
		state.setOrigin(origin);
		state.setTitle(origin.getTitle());
		// TODO obtain group from vertex (require Vertex to have group property)
		state.setGroup(null);

		// identical
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());
		state.setIdentical(sameAsNodeCluster);

		// outbound
		Collection<Relationship> outbounds = graphService.getDirectOutboundRelationships(origin.getIdentifier(),
				origin.getIdentifierType());
		state.setOutbounds(outbounds);

		return state;
	}

}
