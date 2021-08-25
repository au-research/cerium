package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyceliumService {

	public static final String IMPORT_REQUEST_TYPE = "mycelium-import";

	public static final String DELETE_REQUEST_TYPE = "mycelium-delete";

	private final GraphService graphService;

	private final RequestService requestService;

	public MyceliumService(GraphService graphService, RequestService requestService) {
		this.graphService = graphService;
		this.requestService = requestService;
	}

	/**
	 * Proxy method to save the {@link Request} via the {@link RequestService}
	 * @param request the {@link Request} to persist
	 * @return the persisted {@link Request}
	 */
	public Request save(Request request) {
		return requestService.save(request);
	}

	/**
	 * Create a new Import Request with data path, log path and xml stored as payload
	 * both import and update should use the same code
	 * update deletes registryObject Vertex before import
	 * @param json the XML payload
	 * @return the created {@link Request}
	 */
	public Request createImportRequest(String json) {
		Request request = new Request();
		request.setType(IMPORT_REQUEST_TYPE);
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());

		request = save(request);

		// create data path
		try {
			Path path = Paths.get(requestService.getDataPathFor(request));
			Files.createDirectories(path);
			request.setAttribute(Attribute.DATA_PATH, path.toAbsolutePath().toString());
		}
		catch (IOException e) {
			log.error("Failed creating data path {}", e.getMessage());
		}

		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));

		// payload path
		String dataPath = requestService.getDataPathFor(request);
		String payloadPath = dataPath + File.separator + "payload";
		request.setAttribute(Attribute.PAYLOAD_PATH, payloadPath);

		// save json to payload
		try {
			Files.createFile(Paths.get(payloadPath));
			Files.writeString(Paths.get(payloadPath), json);
		}
		catch (IOException e) {
			log.error("Failed to write to payload path: {} with content {}", payloadPath, json);
		}

		return request;
	}


	/**
	 * Create a new Delete Request with log path
	 * @param registryObjectId the recordId to be removed from the Graph
	 * @return the created {@link Request}
	 */
	public Request createDeleteRequest(String registryObjectId) {
		Request request = new Request();
		request.setType(DELETE_REQUEST_TYPE);
		request.setCreatedAt(new Date());
		request.setUpdatedAt(new Date());
		request = save(request);
		request.setAttribute(Attribute.RECORD_ID, registryObjectId);
		// log path
		request.setAttribute(Attribute.LOG_PATH, requestService.getLoggerPathFor(request));
		return request;
	}



	public void validateRequest(Request request) {
		// todo migrate this method and tests to MyceliumRequestService

		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		if (payloadPath == null) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// validate payload
		String payload;
		try {
			payload = Helpers.readFile(payloadPath);
		}
		catch (IOException e) {
			throw new ContentNotSupportedException("Inaccessible payload file");
		}

		// test payload is empty
		if (payload.isBlank()) {
			throw new ContentNotSupportedException("Payload is empty");
		}

		// test for payload syntax error(s)
		try {
			JSONObject jsonObject = new JSONObject(payload);
		}
		catch (JSONException e) {
			throw new ContentNotSupportedException("Payload is not well-formed JSON ");
		}

		// todo test payload is rifcs
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
		if(original != null) {
			graphService.deleteVertex(original);
		}

		graphService.ingestGraph(graph);
	}

	public void ingest(String payload, Request request) {

		StopWatch stopWatch = new StopWatch("Ingest payload");

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();

		// creates the graph out of the xml
		stopWatch.start("CreatingGraph");
		Graph graph = null;
		try {


			ObjectMapper mapper = new ObjectMapper();
			RegistryObject ro = mapper.readValue(payload, RegistryObject.class);
			String recordId = ro.getRegistryObjectId().toString();

			RecordState recordStateBefore = getRecordState(recordId);


			stopWatch.stop();
			request.setAttribute("RECORD_STATE_BEFORE", recordStateBefore.toString());
			Vertex origin = recordStateBefore.getOrigin();
			if(origin != null) {
				// simply deleting the original Vertex Neo4J deletes all relationships to and from the node
				// no need to remove non returning relationships and attributes
				graphService.deleteVertex(origin);
			}
			// create the new Graph
			
			graph = graphProvider.get(ro);
			// insert into neo4j the generated Graph
			stopWatch.start("IngestingGraph");
			graphService.ingestGraph(graph);
			stopWatch.stop();

			log.debug(stopWatch.prettyPrint());

			List<Vertex> registryObjectVertices = graph.getVertices().stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());
			if(origin != null) {
				request.setSummary(String.format("Update: %s Vertices", registryObjectVertices.size()));
			}else{
				request.setSummary(String.format("Created: %s Vertices", registryObjectVertices.size()));
			}

			RecordState recordStateAfter = getRecordState(recordId);
			request.setAttribute("RECORD_STATE_AFTER", recordStateAfter.toString());
		}
		catch (Exception e) {
			log.error("Failed creating graph for payload. Reason: {}", e.toString());
		}

		// implicit duplicate records generation for all vertices that is a RegistryObject
		// graphService.generateDuplicateRelationships(registryObjectVertices);

		// todo implicit GrantsNetwork
	}

	public void deleteRecord(String recordId, Request request){
		// recordState should contain information before delete

		RecordState recordStateBefore = getRecordState(recordId);
		// TODO we should either capture the before and after states and act on them right after import and delete
		// or store them in the Request and let a future task handle them
		request.setAttribute("RECORD_STATE_BEFORE", recordStateBefore.toString());
		Vertex origin = recordStateBefore.getOrigin();

		//Collection<Vertex> sameAs = recordStateBefore.getIdentical();
		// should we delete Identifiers if there are no relationships connected to sameAs or otherwise?
		// all outbound relationships (after same as were removed) must be removed (but the target vertices should not be tested for removal

		if(origin != null) {
			graphService.deleteVertex(origin);
			request.setSummary(String.format("Deleted Record with ID %s", recordId));
		}else{
			request.setSummary(String.format("Record with ID %s doesn't exist", recordId));
		}

		RecordState recordStateAfter = getRecordState(recordId);
		request.setAttribute("RECORD_STATE_AFTER", recordStateAfter.toString());

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
	 * @return the {@link RecordState} or null if the RegistryObject is not present in the database
	 */
	public RecordState getRecordState(String registryObjectId) {

		// if the registryObjectId doesn't exist in the graph
		RecordState state = new RecordState();
		Vertex origin = graphService.getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (origin == null) {
			return state;
		}



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
