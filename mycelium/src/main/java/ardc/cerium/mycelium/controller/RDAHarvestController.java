package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.service.RDAHarvestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/rda")
public class RDAHarvestController {

	@Autowired
	RDAHarvestingService rdaHarvestingService;

	@GetMapping("/harvest")
	public ResponseEntity<?> harvest(@RequestParam String rdaUrl) {
		rdaHarvestingService.harvest(rdaUrl);
		return ResponseEntity.ok("Done");
	}

}
