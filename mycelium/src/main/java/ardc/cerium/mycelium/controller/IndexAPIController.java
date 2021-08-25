package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class IndexAPIController {

	private final MyceliumService myceliumService;

	private final MyceliumIndexingService myceliumIndexingService;

	public IndexAPIController(MyceliumService myceliumService, MyceliumIndexingService myceliumIndexingService) {
		this.myceliumService = myceliumService;
		this.myceliumIndexingService = myceliumIndexingService;
	}

	@PostMapping("/index-record")
	public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {

		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
		log.debug("Indexing {}", from);
		myceliumIndexingService.indexVertex(from);

		return ResponseEntity.ok("Done!");
	}

}
