package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.function.Predicate;

@RestController
@RequestMapping(value = "/api/resources/mycelium-registry-objects")
@Slf4j
@RequiredArgsConstructor
public class MyceliumRegistryObjectResourceController {

	private final MyceliumService myceliumService;

	// todo implement GET /
	@GetMapping(path = "")
	public ResponseEntity<?> getAllRegistryObjects() {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	// todo implement POST /
	@PostMapping(path = "")
	public ResponseEntity<?> importRegistryObject() {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Get the registryObject Vertex
	 * todo Accept: application/json+ardc-ro
	 * @param registryObjectId id of the registry object
	 * @return the {@link Vertex} representation of the RegistryObject
	 */
	@GetMapping(path = "/{registryObjectId}")
	public ResponseEntity<Vertex> getRegistryObjectVertex(@PathVariable("registryObjectId") String registryObjectId) {
		return ResponseEntity.ok().body(myceliumService.getVertexFromRegistryObjectId(registryObjectId));
	}

	// todo implement DELETE /{id}
	@DeleteMapping(path = "/{registryObjectId}")
	public ResponseEntity<?> deleteRegistryObject(@PathVariable("registryObjectId") String registryObjectId) {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
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

}
