package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.model.dto.RegistryObjectVertexDTO;
import ardc.cerium.mycelium.model.dto.VertexDTO;
import ardc.cerium.mycelium.model.mapper.RegistryObjectVertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexDTOMapper;
import ardc.cerium.mycelium.model.mapper.VertexMapper;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
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

import java.util.Collection;
import java.util.function.Predicate;

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

	// todo implement GET /{id}/duplicates
	@GetMapping(path = "/{registryObjectId}/duplicates")
	public ResponseEntity<?> getRegistryObjectDuplicates(@PathVariable("registryObjectId") String registryObjectId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	// todo implement GET /{id}/local-graph
	@GetMapping(path = "/{registryObjectId}/local-graph")
	public ResponseEntity<?> getRegistryObjectLocalGraph(@PathVariable("registryObjectId") String registryObjectId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
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
