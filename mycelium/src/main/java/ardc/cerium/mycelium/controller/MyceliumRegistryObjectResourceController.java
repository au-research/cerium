package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.exception.NotFoundException;
import ardc.cerium.mycelium.model.*;
import ardc.cerium.mycelium.model.dto.TreeNodeDTO;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.TreeNodeDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumService;
import com.google.common.base.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
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

	private final TreeNodeDTOMapper treeNodeDTOMapper;

	@GetMapping(path = "")
	public ResponseEntity<Page<?>> getAllRegistryObjects(@RequestHeader(name="Accept") String acceptHeader, Pageable pageable) {
		Page<Vertex> result = myceliumService.getGraphService().getAllRegistryObjects(pageable);

		Converter converter = getConverterFromAcceptHeader(acceptHeader);
		Page<?> resultDTO = result.map(converter);
		return ResponseEntity.ok(resultDTO);
	}

	/**
	 * Import RegistryObject
	 *
	 * Handles POST /api/resources/mycelium-registry-objects
	 * @param json the JSON Payload that deserialised into a {@link RegistryObject}
	 * @param requestId an optional RequestID to track SideEffect
	 * @return a {@link Request} of the Import
	 */
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
	 * Get the RegistryObject by ID
	 *
	 * Handles GET /api/resources/mycelium-registry-objects/{registryObjectId}
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

	/**
	 * Delete a RegistryObject by ID
	 *
	 * Handles DELETE /api/resources/mycelium-registry-objects/{registryObjectId}
	 * @param registryObjectId the registryObjectId to be deleted from the Graph
	 * @param requestId an optional RequestID to track SideEffect
	 * @return the {@link Request} of the Deletion
	 */
	@DeleteMapping(path = "/{registryObjectId}")
	public ResponseEntity<?> deleteRegistryObject(@PathVariable("registryObjectId") String registryObjectId,
			@RequestParam(required = false) String requestId) {
		log.info("Deleting RegistryObject[id={}, requestId={}]", registryObjectId, requestId);

		Request request = myceliumService.getMyceliumRequestService().findById(requestId);

		// run the DeleteTask
		myceliumService.runDeleteTask(registryObjectId, request);

		return ResponseEntity.ok(request);
	}

	/**
	 * Get All Identifiers for a given RegistryObject
	 *
	 * Handles GET /api/resources/mycelium-registry-objects/{registryObjectId}/identifiers
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

	/**
	 * Get RegistryObject duplicates or Identical Records
	 *
	 * Handles GET /api/resources/mycelium-registry-objects/{registryObjectId}/duplicates
	 * @param registryObjectId id of the registry object
	 * @return a {@link Collection} of Vertices
	 */
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

	/**
	 * Obtain a Graph of the RegistryObject
	 *
	 * Handles GET /api/resources/mycelium-registry-objects/{registryObjectId}/graphs
	 * @param registryObjectId the RegistryObject ID of the origin Vertex
	 * @param includeReverseExternal whether to include reverse external relationships
	 * @param includeReverseInternal whether to include reverse internal relationships
	 * @param includeDuplicates whether to include isSameAs relationships and vertices
	 * @param includeGrantsNetwork whether to include the GrantsNetwork path upwards
	 * @param includeInterLinking whether to include relationships found between any of
	 * the visible nodes (defaults to true)
	 * @param includeCluster whether to include cluster nodes when
	 * @return a {@link Graph} with all relevant vertices and edges
	 */
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

	@GetMapping(path = "/{registryObjectId}/nested-collection-parents")
	public ResponseEntity<?> getNestedCollectionParents(@PathVariable("registryObjectId") String registryObjectId,
			@RequestParam(required = false, defaultValue = "100") String limitChildrenCount,
			@RequestParam(required = false, defaultValue = "100") String limitSiblingCount) {
		Integer childrenSizeLimit = Integer.parseInt(limitChildrenCount);
		Integer siblingSizeLimit = Integer.parseInt(limitSiblingCount);

		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		if (from == null) {
			throw new NotFoundException(String.format("Record ID: {} is not found", registryObjectId));
		}

		Graph graph = myceliumService.getGraphService().getNestedCollectionParents(from);

		// get a map of all the nodes based on the vertices
		Map<String, TreeNodeDTO> nodes = graph.getVertices().stream()
				.filter(vertex -> vertex.getObjectClass().equals("collection")).map(vertex -> {
			return treeNodeDTOMapper.getConverter().convert(vertex);
		}).collect(Collectors.toMap(TreeNodeDTO::getIdentifier, Function.identity()));

		// current node should be put in regardless
		nodes.put(from.getIdentifier().toString(), treeNodeDTOMapper.getConverter().convert(from));

		// get and set children for all the nodes based on the edges
		graph.getEdges().forEach(edge -> {
			// edges should be in the form of (from)-[isPartOf]->(to)
			// that means (from) is a children of (to)
			TreeNodeDTO parent = nodes.get(edge.getTo().getIdentifier().toString());
			TreeNodeDTO child = nodes.get(edge.getFrom().getIdentifier().toString());
			if (parent == null || child == null) {
				return;
			}
			if (! parent.getChildren().contains(child)) {
				parent.getChildren().add(child);
			}

			// and (to) is a parent of (from)
			child.setParentId(parent.getIdentifier());
		});

		// exclude my duplicates (avoid cycles)
		Collection<Vertex> duplicateRegistryObject = myceliumService.getGraphService().getSameAs(from.getIdentifier(), from.getIdentifierType());
		List<String> duplicateIDs = duplicateRegistryObject.stream().map(vertex -> {
			return vertex.getIdentifier();
		}).collect(Collectors.toList());

		// exclude duplicateIDs
		List<String> excludeIDs = new ArrayList<>();
		excludeIDs.addAll(duplicateIDs);

		// children of the originNode
		Collection<Relationship> relationships = myceliumService.getGraphService().getNestedCollectionChildren(from,
				childrenSizeLimit, 0, excludeIDs);
		List<TreeNodeDTO> children = relationships.stream().map(relationship -> {
			TreeNodeDTO dto = new TreeNodeDTO();
			Vertex target = relationship.getTo();
			if (target.getIdentifierType().equals("ro:key")) {
				target = myceliumService.getRegistryObjectVertexFromKey(target.getIdentifier());
			}
			return treeNodeDTOMapper.getConverter().convert(target);
		}).map(dto -> {
			Integer childrenCount = myceliumService.getGraphService().getNestedCollectionChildrenCount(dto.getIdentifier(), new ArrayList<>());
			dto.setChildrenCount(childrenCount);
			log.debug("Children Count of RegistryObjectId[id={}] is {}", dto.getIdentifier(), childrenCount);
			return dto;
		}).collect(Collectors.toList());
		nodes.get(from.getIdentifier().toString()).setChildren(children);

		TreeNodeDTO originNode = nodes.get(from.getIdentifier().toString());

		// direct parent
		Vertex directParentVertex = myceliumService.getVertexFromRegistryObjectId(originNode.getParentId());
		if (directParentVertex != null) {
			Collection<Relationship> siblingRelationship = myceliumService.getGraphService()
					.getNestedCollectionChildren(directParentVertex, siblingSizeLimit, 0, excludeIDs);

			// siblings
			List<TreeNodeDTO> siblings = siblingRelationship.stream().map(relationship -> {
				Vertex target = relationship.getTo();
				if (target.getIdentifierType().equals("ro:key")) {
					target = myceliumService.getRegistryObjectVertexFromKey(target.getIdentifier());
				}
				return treeNodeDTOMapper.getConverter().convert(target);
			}).filter(dto -> {
				// filter out siblings that is the same as the original node or non existence
				return dto != null && !dto.getIdentifier().equals(originNode.getIdentifier());
			}).map(dto -> {
				Integer childrenCount = myceliumService.getGraphService().getNestedCollectionChildrenCount(dto.getIdentifier(), new ArrayList<>());
				dto.setChildrenCount(childrenCount);
				log.debug("Children Count of RegistryObjectId[id={}] is {}", dto.getIdentifier(), childrenCount);
				return dto;
			}).collect(Collectors.toList());

			nodes.get(originNode.getParentId()).getChildren().addAll(siblings);
		}

		// set children count for initial nodes
		nodes.entrySet().stream().forEach(entry -> {
			String id = entry.getValue().getIdentifier();

			// special case (for parents), if it has exactly 1 children, that's the initial load and that child should be excluded from the count
			List<String> excludeIdentifiers = new ArrayList<>();
			if (entry.getValue().getChildren().size() == 1) {
				excludeIdentifiers = entry.getValue().getChildren().stream().map(dto -> {
					return dto.getIdentifier().toString();
				}).collect(Collectors.toList());
			}

			Integer childrenCount = myceliumService.getGraphService().getNestedCollectionChildrenCount(id, excludeIdentifiers);
			entry.getValue().setChildrenCount(childrenCount);
//			log.debug("Children Count of RegistryObjectId[id={}] is {}", entry.getValue().getIdentifier(),
//					childrenCount);
		});

		// top node is the node without a parent
		TreeNodeDTO topNode = nodes.entrySet().stream().map(map -> map.getValue()).filter(node -> {
			return node.getParentId() == null;
		}).findFirst().orElse(null);

		return ResponseEntity.ok().body(topNode);

	}

	@GetMapping(path = "/{registryObjectId}/nested-collection-children")
	public ResponseEntity<?> getNestedCollectionChildren(@PathVariable("registryObjectId") String registryObjectId,
			@RequestParam(required = false, defaultValue = "100") String limit,
			@RequestParam(required = false, defaultValue = "0") String offset,
			@RequestParam(required = false, defaultValue = "") String excludeIdentifiers) {
		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		int iOffSet = Integer.parseInt(offset);
		/*
		avoid offset being less than 0
		 */
		if(iOffSet < 0){
			iOffSet = 0;
		}
		// exclude my duplicates (avoid cycles)
		Collection<Vertex> duplicateRegistryObject = myceliumService.getGraphService().getSameAs(from.getIdentifier(), from.getIdentifierType());
		List<String> duplicateIDs = duplicateRegistryObject.stream().map(vertex -> {
			return vertex.getIdentifier();
		}).collect(Collectors.toList());

		List<String> excludeIDs = new ArrayList<>();
		if (excludeIdentifiers != "") {
			Arrays.asList(excludeIdentifiers.split("\\s*,\\s*")).forEach(identifer -> {
				List<String> excludedIdentifiers = myceliumService.getGraphService().getSameAs(identifer, "ro:id").stream().map(v -> {
					return v.getIdentifier();
				}).collect(Collectors.toList());
				excludeIDs.addAll(excludedIdentifiers);
			});
		}
		excludeIDs.addAll(duplicateIDs);

		GraphService graphService = myceliumService.getGraphService();
		Collection<Relationship> relationships = graphService.getNestedCollectionChildren(from, Integer.parseInt(limit),
				iOffSet, excludeIDs);

		List<TreeNodeDTO> children = relationships.stream().map(relationship -> {
			Vertex target = relationship.getTo();
			if (target.getIdentifierType().equals("ro:key")) {
				target = myceliumService.getRegistryObjectVertexFromKey(target.getIdentifier());
			}
			return treeNodeDTOMapper.getConverter().convert(target);
		}).collect(Collectors.toList());

		// set children count
		children.forEach(entry -> {
			String id = entry.getIdentifier();
			entry.setChildrenCount(myceliumService.getGraphService().getNestedCollectionChildrenCount(id, new ArrayList<>()));
		});

		return ResponseEntity.ok().body(children);

	}

	/**
	 * Obtain the {@link Converter} to be used with an Accept header.
	 * @param acceptHeader the Accept header
	 * @return the {@link Converter} to be used to render returned values
	 */
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
