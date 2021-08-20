package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class IndexAPIController {

	@Autowired
	MyceliumService myceliumService;

	@Autowired
	MyceliumIndexingService myceliumIndexingService;

	@PostMapping("/index-record")
	public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {

		Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);

		myceliumIndexingService.indexVertex(from);

		return ResponseEntity.ok("Done!");
	}

}
