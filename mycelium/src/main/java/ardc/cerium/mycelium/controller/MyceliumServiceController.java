package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.mycelium.model.Edge;
import ardc.cerium.mycelium.model.Graph;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.GraphService;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
@RequiredArgsConstructor
public class MyceliumServiceController {

	private final MyceliumService myceliumService;

	/**
	 * Import an XML payload to the {@link MyceliumService}
	 * @param json the JSON payload
	 * @param sideEffectRequestID the Affected Relationship Request ID
	 * @return the {@link ResponseEntity} of a {@link Request}
	 */
	@PostMapping("/import-record")
	public ResponseEntity<Request> importRecord(@RequestBody String json, @RequestParam String sideEffectRequestID) {
		log.info("Importing Record sideEffectRequestId={}", sideEffectRequestID);
		log.debug("Received Import Request [sideEffectRequestId={}, payload={}]", sideEffectRequestID, json);

		// create new Request, store the json payload
		RequestDTO dto = new RequestDTO();
		dto.setType(MyceliumRequestService.IMPORT_REQUEST_TYPE);
		Request request = myceliumService.createRequest(dto);
		request.setAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID, sideEffectRequestID);

		// store the json payload
		myceliumService.saveToPayloadPath(request, json);
		request.setStatus(Request.Status.ACCEPTED);
		myceliumService.save(request);

		myceliumService.validateRequest(request);

		// create the import task and run it immediately
		myceliumService.runImportTask(request);

		request = myceliumService.save(request);
		return ResponseEntity.ok(request);
	}

	@PostMapping("/index-record")
	public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {
		log.info("Indexing RegistryObject[id={}]", registryObjectId);
		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		if (from == null) {
			log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
			return ResponseEntity.badRequest()
					.body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
		}
		log.debug("Indexing Vertex[identifier={}]", from.getIdentifier());
		myceliumService.indexVertex(from);
		log.debug("Index completed Vertex[identifier={}]", from.getIdentifier());

		return ResponseEntity.ok("Done!");
	}

	/**
	 * Delete a RegistryObject by ID
	 * @param registryObjectId the registryObjectId to be deleted from the Graph
	 * @param sideEffectRequestID the requestId of the side effect Request
	 * @return a {@link ResponseEntity} of a {@link Request}
	 */
	@PostMapping("/delete-record")
	public ResponseEntity<Request> deleteRecord(@RequestParam String registryObjectId,
			@RequestParam String sideEffectRequestID) {
		log.info("Deleting RegistryObject[id={}, sideEffectRequestId={}]", registryObjectId, sideEffectRequestID);

		// create and save the request
		RequestDTO dto = new RequestDTO();
		dto.setType(MyceliumRequestService.DELETE_REQUEST_TYPE);
		Request request = myceliumService.createRequest(dto);
		request.setStatus(Request.Status.ACCEPTED);
		request.setAttribute(Attribute.RECORD_ID, registryObjectId);
		request.setAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID, sideEffectRequestID);
		request = myceliumService.save(request);

		// run the DeleteTask
		myceliumService.runDeleteTask(request);

		request = myceliumService.save(request);
		return ResponseEntity.ok(request);
	}

	@PostMapping("/start-queue-processing")
	public ResponseEntity<?> startQueueProcessing(@Parameter(name = "sideEffectRequestId",
			description = "Request ID of the Side Effect Request") String requestId) {
		log.info("Start Queue Processing Request[sideEffectRequestId={}]", requestId);

		Request request = myceliumService.findById(requestId);

		// todo confirm and validate request status

		String queueID = myceliumService.getMyceliumSideEffectService().getQueueID(requestId);
		log.debug("QueueID obtained: {}", queueID);

		request.setStatus(Request.Status.RUNNING);
		myceliumService.save(request);

		// workQueue is an Async method that would set Request to COMPLETED after it has
		// finished
		myceliumService.getMyceliumSideEffectService().workQueue(queueID, request);

		return ResponseEntity.ok().body(request);
	}

	@GetMapping("/get-record-graph")
	public ResponseEntity<?> getRecordGraph(
			@Parameter(name = "registryObjectId", description = "ID of the registryObject") String registryObjectId,
			@RequestParam(defaultValue = "true") boolean includeReverse,
			@RequestParam(defaultValue = "true") boolean includeInternal,
			@RequestParam(defaultValue = "true") boolean includeDuplicates,
			@RequestParam(defaultValue = "true") boolean includeGrantsNetwork,
			@RequestParam(defaultValue = "true") boolean includeInterLinking) {

		log.info("Obtaining graph for RegistryObject[id={}]", registryObjectId);
		Vertex vertex = myceliumService.getGraphService().getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (vertex == null) {
			log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
			return ResponseEntity.badRequest()
					.body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
		}

		// todo includeReverse=true
		// todo includeInternal=true

		GraphService graphService = myceliumService.getGraphService();

		// obtain the immediate relationships, the grants network relationships as graphs
		// and merge the graph together
		Graph graph = new Graph();
		graph.addVertex(vertex);
		graph.mergeGraph(graphService.getRegistryObjectGraph(vertex));

		if (includeGrantsNetwork) {
			graph.mergeGraph(graphService.getGrantsNetworkGraphUpwards(vertex));
		}

		// manually add the Duplicates into the Graph
		if (includeDuplicates) {
			Collection<Vertex> duplicateRegistryObjects = graphService.getDuplicateRegistryObject(vertex);
			duplicateRegistryObjects.stream().filter(v -> !v.getId().equals(vertex.getId())).forEach(duplicate -> {
				graph.addVertex(duplicate);
				graph.addEdge(new Edge(vertex, duplicate, RIFCSGraphProvider.RELATION_SAME_AS));
			});
		}

		// interlinking between
		if (includeInterLinking) {
			List<Vertex> otherDirectlyRelatedVertices = graph.getVertices().stream()
					.filter(v -> !v.getIdentifier().equals(vertex.getIdentifier())).collect(Collectors.toList());
			graph.mergeGraph(graphService.getGraphBetweenVertices(otherDirectlyRelatedVertices));
		}

		// todo includeInterLinking=true
		// todo includeCluster=true

		// clean up the data before returning
		graphService.removeDanglingVertices(graph);

		return ResponseEntity.ok(graph);
	}

	@PostMapping("/regen-grants-network-relationships")
	public ResponseEntity<?> regenerateGrantsNetworkRelationships(
			@Parameter(name = "registryObjectId", description = "ID of the registryObject") String registryObjectId,
			@Parameter(name = "show") boolean show) {
		log.info("Regenerate Grants Network Relationships RegistryObject[id={}]", registryObjectId);
		Vertex vertex = myceliumService.getGraphService().getVertexByIdentifier(registryObjectId,
				RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		if (vertex == null) {
			throw new RecordNotFoundException(registryObjectId);
		}

		myceliumService.getMyceliumIndexingService().regenGrantsNetworkRelationships(vertex);

		if (!show) {
			return ResponseEntity.ok().body("OK");
		}

		Cursor<RelationshipDocument> cursor = myceliumService.getMyceliumIndexingService()
				.cursorFor(new Criteria("from_id").is(registryObjectId));
		List<RelationshipDocument> relationshipDocuments = new ArrayList<>();
		while (cursor.hasNext()) {
			RelationshipDocument doc = cursor.next();
			relationshipDocuments.add(doc);
		}
		return ResponseEntity.ok().body(relationshipDocuments);
	}

	@GetMapping("/get-duplicate-records")
	public ResponseEntity<?> getDuplicateRecord(@RequestParam String registryObjectId) {
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

}
