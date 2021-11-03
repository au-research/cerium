package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.RDAHarvestingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping(value = "/api/admin/rda")
public class RDAHarvestController {

	@Autowired
	RDAHarvestingService rdaHarvestingService;

	@Autowired
	MyceliumService myceliumService;

	@GetMapping("/harvest")
	public ResponseEntity<?> harvest(@RequestParam String rdaUrl) {
		rdaHarvestingService.harvest(rdaUrl);
		return ResponseEntity.ok("Done");
	}

	@GetMapping("/test")
	public ResponseEntity<?> test() {

		try (Stream<Vertex> stream = myceliumService.getGraphService().streamRegistryObjectFromDataSource("1")) {
			stream.forEach(ro -> {
				log.info("RegistryObject[id={}, type={}]", ro.getIdentifier(), ro.getIdentifierType());
			});
		}

		return ResponseEntity.ok("Done");
	}

}
