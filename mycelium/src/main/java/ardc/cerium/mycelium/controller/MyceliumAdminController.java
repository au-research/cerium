package ardc.cerium.mycelium.controller;

import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumVertexResolvingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping(value = "/api/admin/mycelium")
@Slf4j
@RequiredArgsConstructor
public class MyceliumAdminController {

	private final MyceliumService myceliumService;

	private final MyceliumVertexResolvingService vertexResolvingService;

	@PostMapping("/resolve-vertices")
	public ResponseEntity<?> resolveVertices() {
		log.info("Resolve all vertices - on demand");

		vertexResolvingService.resolveAllVertices();

		return ResponseEntity.ok("");
	}

	@GetMapping("/info")
	public ResponseEntity<?> getSystemInfo(@RequestParam(required = false, defaultValue = "brief") String level) {
		HashMap<String, Object> info = new HashMap<>() ;
		info.put("infoLevel", level);
		// TODO: add more system wide stats and info
		log.info("Generating {} System Information", level);
		// 10 minutes should be enough
		int sleepMillies = 3000; // 3 second
		int retryCount = 200; // x20
		try {
			myceliumService.getGraphService().verifyConnectivity(sleepMillies,retryCount);
			myceliumService.getSystemInfo(info);
			return ResponseEntity.ok().body(info);
		}catch(Exception e){
			log.warn(e.getMessage());
			return ResponseEntity.badRequest().body(String.format(e.getMessage()));
		}
	}
}
