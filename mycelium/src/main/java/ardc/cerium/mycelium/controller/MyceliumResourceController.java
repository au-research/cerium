package ardc.cerium.mycelium.controller;


import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.provider.RIFCSGraphProvider;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.function.Predicate;

@RestController
@RequestMapping(value = "/resources")
@Slf4j
@RequiredArgsConstructor
public class MyceliumResourceController {

    private final MyceliumService myceliumService;

    /**
	 *
	 * Get the Identifiers of a registryObject
	 * get all that is sameAs and remove key and ro vertices
	 */
	@GetMapping(path="/records/{roID}/identifiers")
	public ResponseEntity<?> getNestedChildren(@PathVariable("roID") String roID)
{
		Collection<Vertex> duplicateRegistryObject = myceliumService.getGraphService().getSameAs(roID, "ro:id");
		Predicate<Vertex> isRecord = v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_ID_IDENTIFIER_TYPE);
		Predicate<Vertex> isKey= v -> v.getIdentifierType().equals(RIFCSGraphProvider.RIFCS_KEY_IDENTIFIER_TYPE);
		duplicateRegistryObject.removeIf(isRecord.or(isKey));
		return ResponseEntity.ok().body(duplicateRegistryObject);
	}

	/**
	 *
	 * Get the registryObject Vertex
	 */
	@GetMapping(path="/records/{roID}")
	public ResponseEntity<?> getRegistryObjectVertex(@PathVariable("roID") String roID)
	{
		return ResponseEntity.ok().body(myceliumService.getVertexFromRegistryObjectId(roID));
	}


}
