package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import org.redisson.api.RQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping(value = "/api/resources/mycelium-requests",
		produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
public class MyceliumRequestResourceController {

	@Autowired
	MyceliumRequestService myceliumRequestService;

	@Autowired
	MyceliumSideEffectService myceliumSideEffectService;

	@PostMapping(value = "/")
	public ResponseEntity<RequestDTO> store(@RequestBody RequestDTO requestDTO) throws Exception {
		myceliumRequestService.validateRequestDTO(requestDTO);
		Request request = myceliumRequestService.createRequest(requestDTO);

		RequestDTO dto = myceliumRequestService.getRequestMapper().getConverter().convert(request);
		URI location = URI.create("/api/resources/requests/" + dto.getId().toString());
		return ResponseEntity.created(location).body(dto);
	}

	@GetMapping(value = "/{id}")
	public ResponseEntity<RequestDTO> show(@PathVariable String id) {
		Request request = myceliumRequestService.findById(id);

		RequestDTO dto = myceliumRequestService.getRequestMapper().getConverter().convert(request);
		return ResponseEntity.ok().body(dto);
	}

	@GetMapping(value = "/{id}/logs")
	public ResponseEntity<String> showLogs(@PathVariable String id) throws IOException {
		Request request = myceliumRequestService.findById(id);

		String logPath = myceliumRequestService.getRequestService().getLoggerPathFor(request);
		File logFile = new File(logPath);
		if (!logFile.exists()) {
			throw new RuntimeException(String.format("Logs Path: %s doesn't exist", logPath));
		}
		String logContent = Helpers.readFile(logPath);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(logContent);
	}

	@GetMapping(value = "/{id}/queue")
	public ResponseEntity<?> showQUeue(@PathVariable String id) {
		Request request = myceliumRequestService.findById(id);
		if (! request.getType().equals(MyceliumRequestService.AFFECTED_REL_REQUEST_TYPE)) {
			throw new RuntimeException("Queue is only applicable for "
					+ MyceliumRequestService.AFFECTED_REL_REQUEST_TYPE + " type Request");
		}

		String queueID = myceliumSideEffectService.getQueueID(request.getId().toString());
		RQueue<SideEffect> sideEffectRQueue = myceliumSideEffectService.getQueue(queueID);

		return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(sideEffectRQueue);
	}

}
