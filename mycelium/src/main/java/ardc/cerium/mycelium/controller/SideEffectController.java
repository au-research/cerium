package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class SideEffectController {

	@Autowired
	MyceliumSideEffectService myceliumSideEffectService;

	@Autowired
	MyceliumRequestService myceliumRequestService;

	@PostMapping("/start-queue-processing")
	public ResponseEntity<?> startQueueProcessing(@Parameter(name = "sideEffectRequestId",
			description = "Request ID of the Side Effect Request") String requestId) {

		log.debug("Received request to process SideEffectQueue Request[id={}]", requestId);

		Request request = myceliumRequestService.findById(requestId);

		// todo confirm and validate request status

		String queueID = myceliumSideEffectService.getQueueID(requestId);
		log.debug("QueueID obtained: {}", queueID);

		request.setStatus(Request.Status.RUNNING);
		myceliumRequestService.save(request);

		// workQueue is an Async method that would set Request to COMPLETED after it has finished
		myceliumSideEffectService.workQueue(queueID, request);

		return ResponseEntity.ok().body(request);
	}

}
