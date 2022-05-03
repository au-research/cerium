package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumVertexResolvingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
