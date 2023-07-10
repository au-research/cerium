package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.model.solr.RelationshipDocument;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.rifcs.model.Identifier;
import ardc.cerium.mycelium.service.IdentifierNormalisationService;
import ardc.cerium.mycelium.service.MyceliumService;
import com.google.common.base.Converter;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
@RequiredArgsConstructor
public class MyceliumServiceController {

	private final MyceliumService myceliumService;
	private final VertexDTOMapper vertexMapper;

	private final IdentifierNormalisationService identifierNormalisationService;

	/**
	 * Index a RegistryObject by ID
	 * @param registryObjectId the RegistryObject ID to index
	 * @return a String response
	 */
	@PostMapping("/index-record")
	public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {
		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		try {
			if (from == null) {
				log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
				return ResponseEntity.badRequest()
						.body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
			}
			log.info("Indexing RegistryObject[id={}]", registryObjectId);
			myceliumService.indexVertex(from);
			log.debug("Index completed Vertex[identifier={}]", from.getIdentifier());
			return ResponseEntity.ok("Done!");
		}catch(Exception e){
			log.warn("There was an Error while indexing m={} trying if Neo4j is unavailable", e.getMessage());

			try {
				int sleepMillies = 3000; // 3 second
				int retryCount = 200; // x20
				// check if neo4j went away
				myceliumService.getGraphService().verifyConnectivity(sleepMillies,retryCount);
				myceliumService.indexVertex(from);
				log.debug("Index completed Vertex[identifier={}]", registryObjectId);

				// todo formulate a formal response, Request?
				return ResponseEntity.ok("Done!");
			}catch(Exception f) {
				return ResponseEntity.badRequest().body(String.format("Error Indexing record wirh ro_id=%s error=%s",registryObjectId, f.getMessage()));
			}
		}
	}

	/**
	 * Starts the SideEffectQueue processing asynchronously.
	 *
	 * The progress can be monitored by polling the RequestID via the
	 * {@link MyceliumRequestResourceController}
	 * @param requestId the uuid of the SideEffectRequestID
	 * @return the {@link Request} with the current status updated to RUNNING
	 */
	@PostMapping("/start-queue-processing")
	public ResponseEntity<Request> startQueueProcessing(@RequestBody String importedRecordIds,
			@Parameter(name = "requestId", description = "Request ID of the RequestID") String requestId) {

		log.info("Start Queue Processing Request[requestId={}]", requestId);

		Request request = myceliumService.findById(requestId);

		String queueID = myceliumService.getMyceliumSideEffectService().getQueueID(requestId);
		log.debug("QueueID obtained: {}", queueID);

		request.setStatus(Request.Status.RUNNING);
		myceliumService.save(request);

		// workQueue is an Async method that would set Request to COMPLETED after it has
		// finished
		myceliumService.getMyceliumSideEffectService().workQueueAsync(queueID, request, importedRecordIds);

		return ResponseEntity.ok().body(request);
	}

	/**
	 * Helper API to regenerate a GrantsNetworkRelationships Only for a given
	 * RegistryObjectId
	 *
	 * Development/Testing API.
	 * @param registryObjectId the RegistryObject Id to have GrantsNetwork edges
	 * regenerated in SOLR
	 * @param show whether to show the RelationshipsDocument generated (defaults to false)
	 * @return a string response OK when all is good
	 */
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

	/**
	 * API to return the resolved identifier VertexDTO
	 *
	 * @param value the value of the identifier
	 * @param type the type of the identifier
	 * @return a string response
	 */
	@GetMapping(path = "/resolve-identifiers")
	public ResponseEntity<?> getIdentifierVertex(
			@RequestParam("value") String value,
			@RequestParam("type") String type) {
		Vertex result = myceliumService.getGraphService().getVertexByIdentifier(value,type);
		Converter converter = vertexMapper.getConverter();
		return ResponseEntity.ok().body(converter.convert(result));
	}

	/**
	 * API to return the resolved identifier VertexDTO
	 *
	 * @param value the value of the identifier
	 * @param type the type of the identifier
	 * @return a string response
	 */
	@GetMapping(path = "/normalise-identifiers")
	public ResponseEntity<?> getNormalisedIdentifier(
			@RequestParam("value") String value,
			@RequestParam(required = false, defaultValue = "url") String type) {
		Identifier identifier = new Identifier();
		identifier.setType(type);
		identifier.setValue(value);
		log.debug("Raw Identifier[value={}, type={}]", identifier.getValue(), identifier.getType());
		identifier = IdentifierNormalisationService.getNormalisedIdentifier(identifier);
		log.debug("Normalised Identifier[value={}, type={}]", identifier.getValue(), identifier.getType());
		return ResponseEntity.ok().body(identifier);
	}

}
