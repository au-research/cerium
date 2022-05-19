package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.RelationTypeGroup;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.RegistryObjectVertexDTO;
import ardc.cerium.mycelium.model.dto.VertexDTO;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import com.google.common.base.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/resources/mycelium-registry-objects",
		produces = { "application/json", "application/vnd.ardc.vertex.ro+json" })
@Slf4j
@RequiredArgsConstructor
public class MyceliumRegistryObjectResourceController {

	private final MyceliumService myceliumService;

	private final RegistryObjectVertexDTOMapper roMapper;

	private final VertexDTOMapper vertexMapper;

	@GetMapping(path = "")
	public ResponseEntity<Page<?>> getAllRegistryObjects(@RequestHeader(name="Accept") String acceptHeader, Pageable pageable) {
		Page<Vertex> result = myceliumService.getGraphService().getAllRegistryObjects(pageable);

		Converter converter = getConverterFromAcceptHeader(acceptHeader);
		Page<?> resultDTO = result.map(converter);
		return ResponseEntity.ok(resultDTO);
	}

	@PostMapping(path = "")
	public ResponseEntity<?> importRegistryObject(@RequestBody String json,
			@RequestParam(required = false) String requestId) {
		log.info("Importing Record requestId={}", requestId);

		Request request = requestId != null ? myceliumService.getMyceliumRequestService().findById(requestId) : null;

		// create the import task and run it immediately
		myceliumService.runImportTask(json, request);

		return ResponseEntity.ok(request);
	}

	/**
	 * Get the registryObject Vertex
	 * @param acceptHeader the Accept Header that would determine the output format
	 * @param registryObjectId id of the registry object
	 * @return the {@link Vertex} representation of the RegistryObject
	 */
	@GetMapping(path = "/{registryObjectId}")
	public ResponseEntity<?> getRegistryObjectVertex(@RequestHeader(name="Accept") String acceptHeader, @PathVariable("registryObjectId") String registryObjectId) {
		Vertex vertex = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		Converter converter = getConverterFromAcceptHeader(acceptHeader);
		return ResponseEntity.ok().body(converter.convert(vertex));
	}

	@DeleteMapping(path = "/{registryObjectId}")
	public ResponseEntity<?> deleteRegistryObject(@PathVariable("registryObjectId") String registryObjectId, @RequestParam(required = false) String requestId) {
		log.info("Deleting RegistryObject[id={}, requestId={}]", registryObjectId, requestId);

		Request request = myceliumService.getMyceliumRequestService().findById(requestId);

		// run the DeleteTask
		myceliumService.runDeleteTask(registryObjectId, request);

		return ResponseEntity.ok(request);
	}

	/**
	 * Get All Identifiers for a given RegistryObject
	 * todo Accept: application/json+ardc-identifier
	 * Includes all duplicates identifiers
	 * @param registryObjectId id of the registry object
	 * @return a list of identifiers belonging to the registry object
	 */
	@GetMapping(path = "/{registryObjectId}/identifiers")
	public ResponseEntity<?> getIdentifiers(@PathVariable("registryObjectId") String registryObjectId) {

		// obtaining all Vertex that the registryObject "isSameAs"
		Collection<Vertex> identifiers = myceliumService.getGraphService().getSameAs(registryObjectId, "ro:id");

		// remove all vertices that is a RegistryObject or is a Key Vertex
		Predicate<Vertex> isRecord = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Predicate<Vertex> isKey = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		identifiers.removeIf(isRecord.or(isKey));

		return ResponseEntity.ok().body(identifiers);
	}

	@GetMapping(path = "/{registryObjectId}/duplicates")
	public ResponseEntity<?> getRegistryObjectDuplicates(@PathVariable("registryObjectId") String registryObjectId) {
		log.info("Get Duplicate Record RegistryObject[id={}]", registryObjectId);
		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		if (from == null) {
			log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
			return ResponseEntity.badRequest()
					.body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
		}
		Collection<Vertex> duplicates = myceliumService.getGraphService().getDuplicateRegistryObject(from);
		log.debug("getDuplicates completed Vertex[identifier={}]", from.getIdentifier());
		return ResponseEntity.ok().body(duplicates);
	}

	@GetMapping(path = "/{registryObjectId}/graph")
	public ResponseEntity<?> getRegistryObjectGraph(@PathVariable("registryObjectId") String registryObjectId,
			@RequestParam(defaultValue = "true") boolean includeReverseExternal,
			@RequestParam(defaultValue = "true") boolean includeReverseInternal,
			@RequestParam(defaultValue = "true") boolean includeDuplicates,
			@RequestParam(defaultValue = "true") boolean includeGrantsNetwork,
			@RequestParam(defaultValue = "true") boolean includeInterLinking,
			@RequestParam(defaultValue = "true") boolean includeCluster) {

		log.info("Obtaining graph for RegistryObject[id={}]", registryObjectId);
		Vertex vertex = myceliumService.getGraphService().getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);

		if (vertex == null) {
			log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
			return ResponseEntity.badRequest()
					.body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
		}

		GraphService graphService = myceliumService.getGraphService();

		// todo includeReverseExternal=true
		// todo includeReverseInternal=true

		// obtain the immediate relationships, the grants network relationships as graphs
		// and merge the graph together
		Graph graph = new Graph();
		graph.addVertex(vertex);

		// relationTypeGrouping (clustering)
		Collection<RelationTypeGroup> relationTypeGroups = graphService.getRelationTypeGrouping(vertex);
		List<RelationTypeGroup> overLimitGroups = relationTypeGroups.stream().filter(g -> g.getCount() >= 20)
				.collect(Collectors.toList());

		List<String> overLimitRelationType = new ArrayList<>();
		if (includeCluster) {
			overLimitGroups.forEach(group -> {
				Vertex cluster = new Vertex(UUID.randomUUID().toString(), "ro:cluster");
				cluster.setId(new Random().nextLong());
				cluster.addLabel(Vertex.Label.Cluster);
				group.getLabels().forEach(cluster::addLabel);
				cluster.setObjectClass(group.getObjectClass());
				cluster.setObjectType(group.getObjectType());
				graph.addVertex(cluster);
				graph.addEdge(new Edge(vertex, cluster, group.getRelation(), new Random().nextLong()));
			});
			overLimitRelationType = overLimitGroups.stream().map(RelationTypeGroup::getRelation)
					.collect(Collectors.toList());
			log.debug("OverlimitRelationType: {}", overLimitRelationType);
		}

		// add the immediate relationships (include Duplicate), excludes the
		// overLimitRelationTypes
		graph.mergeGraph(graphService.getRegistryObjectGraph(vertex, overLimitRelationType));

		log.debug("Added registryObjectGraph Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
				graph.getEdges().size());

		// add the GrantsNetworkPath
		if (includeGrantsNetwork) {
			graph.mergeGraph(graphService.getGrantsNetworkGraphUpwards(vertex, overLimitRelationType));
			log.debug("Added grantsNetworkgraphUpwards Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
					graph.getEdges().size());

			graph.mergeGraph(graphService.getGrantsNetworkDownwards(vertex, overLimitRelationType));
			log.debug("Added grantsNetworkgraphDownwards Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
					graph.getEdges().size());
		}

		// manually add the Duplicates into the Graph
		if (includeDuplicates) {
			Collection<Vertex> duplicateRegistryObjects = graphService.getDuplicateRegistryObject(vertex);
			duplicateRegistryObjects.stream().filter(v -> !v.getId().equals(vertex.getId())).forEach(duplicate -> {
				graph.addVertex(duplicate);
				log.debug("Adding duplicates to Graph {}, edges:{}", vertex.getIdentifier(), vertex.getIdentifierType());
				graph.addEdge(new Edge(vertex, duplicate, RIFCSGraphProvider.RELATION_SAME_AS));
			});
			log.debug("Added duplicateGraph Graph[vertex: {}]", duplicateRegistryObjects.size());
		}


		if(vertex.getStatus().equals(Vertex.Status.DRAFT.name())){
			// remove its PUBLISHED for start
			Collection<Vertex> altVersions = graphService.getAltStatusRecord(vertex, Vertex.Status.PUBLISHED.name());
			graph.removeAll(altVersions);
			altVersions = new ArrayList<>(Collections.emptySet());
			for (Vertex v : graph.getVertices()) {
				// remove all DRAFT versions if their PUBLISHED are in the graph
				if (v.getStatus() != null && v.getStatus().equals(Vertex.Status.PUBLISHED.name())) {
					altVersions.addAll(graphService.getAltStatusRecord(v, Vertex.Status.DRAFT.name()));
				}
			}
			graph.removeAll(altVersions);
		}

		// interlinking between current graph vertices
		if (includeInterLinking) {
			List<Vertex> otherDirectlyRelatedVertices = graph.getVertices().stream()
					.filter(v -> !v.getIdentifier().equals(vertex.getIdentifier())).collect(Collectors.toList());

			log.debug("OtherDirectlyRelatedVertices count:{}", otherDirectlyRelatedVertices.size());
			graph.mergeGraph(graphService.getGraphBetweenVertices(otherDirectlyRelatedVertices));
			log.debug("Added interlinkingGraph Graph[vertex: {}, edges:{}]", graph.getVertices().size(),
					graph.getEdges().size());
		}

		// clean up the data

		graphService.removeDanglingVertices(graph);
		log.debug("Removed dangling vertices. Prepare to render graph");

		return ResponseEntity.ok(graph);

	}

	// todo implement GET /{id}/nested-collection-tree
	@GetMapping(path = "/{registryObjectId}/nested-collection-tree")
	public ResponseEntity<?> getNestedCollectionTree(@PathVariable("registryObjectId") String registryObjectId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	// todo implement GET /{id}/nested-collection-graph
	@GetMapping(path = "/{registryObjectId}/nested-collection-children")
	public ResponseEntity<?> getNestedCollectionChildren(@PathVariable("registryObjectId") String registryObjectId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	private Converter getConverterFromAcceptHeader(String acceptHeader) {

		// application/vnd.ardc.vertex.ro+json
		if (acceptHeader.equals("application/vnd.ardc.vertex.ro+json")) {
			return roMapper.converter;
		}

		// application/vnd.ardc.vertex.identifier+json

		// application/json
		return vertexMapper.getConverter();
	}

}
