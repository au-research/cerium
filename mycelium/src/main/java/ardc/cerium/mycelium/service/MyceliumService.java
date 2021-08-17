package ardc.cerium.mycelium.service;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.core.exception.ContentNotSupportedException;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Relationship;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.effect.TitleChangeSideEffect;
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

	public void validateRequest(Request request) {

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

	public void ingest(String payload) {

		StopWatch stopWatch = new StopWatch("Ingest payload");

		// only supports rifcs for now, obtain the graph data from the payload
		RIFCSGraphProvider graphProvider = new RIFCSGraphProvider();

		// creates the graph out of the xml
		stopWatch.start("CreatingGraph");
		Graph graph = null;
		try {
			graph = graphProvider.get(payload);
			stopWatch.stop();

			// insert into neo4j the generated Graph
			stopWatch.start("IngestingGraph");
			graphService.ingestGraph(graph);
			stopWatch.stop();

			log.debug(stopWatch.prettyPrint());

			List<Vertex> registryObjectVertices = graph.getVertices().stream()
					.filter(vertex -> vertex.hasLabel(Vertex.Label.RegistryObject)).collect(Collectors.toList());
			log.info("Created: {} Vertices", registryObjectVertices.size());
		}
		catch (Exception e) {
			log.error("Failed creating graph for payload. Reason: {}", e.toString());
		}

		// implicit duplicate records generation for all vertices that is a RegistryObject
		// graphService.generateDuplicateRelationships(registryObjectVertices);

		// todo implicit GrantsNetwork
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
		Vertex origin = graphService.getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (origin == null) {
			return null;
		}

		RecordState state = new RecordState();
		state.setTitle(origin.getTitle());

		// TODO obtain group from vertex (require Vertex to have group property)
		state.setGroup(null);

		// identical
		Collection<Vertex> sameAsNodeCluster = graphService.getSameAs(origin.getIdentifier(),
				origin.getIdentifierType());
		state.setIdentical(new ArrayList<>(sameAsNodeCluster));

		// outbound
		Collection<Relationship> outbounds = graphService.getDirectOutboundRelationships(origin.getIdentifier(),
				origin.getIdentifierType());
		state.setOutbounds(new ArrayList<>(outbounds));

		return state;
	}

	public List<SideEffect> detectChanges(RecordState before, RecordState after) {
		List<SideEffect> sideEffects = new ArrayList<>();

		// this shouldn't happen
		if (before == null && after == null) {
			return sideEffects;
		}

		// record is created
		if (before == null) {
			// recordCreatedSideEffect
			// investigate after state grants network
			return sideEffects;
		}

		// record is deleted
		if (after == null) {
			// recordDeletedSideEffect
			// investigate before state grants network
			return sideEffects;
		}

		// detect title change
		if (! before.getTitle().equals(after.getTitle())) {
			sideEffects.add(new TitleChangeSideEffect(before.getRegistryObjectId(), before.getTitle(), after.getTitle()));
		}

		// todo other change detection

		return sideEffects;

	}

	public void handleSideEffects(List<SideEffect> sideEffects) {
		sideEffects.forEach(SideEffect::handle);
	}

}
