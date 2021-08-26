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

import java.util.UUID;

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
			description = "Request ID of the Side Effect Request") UUID requestId) {

		Request request = myceliumRequestService.findById(requestId.toString());

		// todo confirm and validate request status, only start processing if the request
		// is in QUEUED state

		String queueID = myceliumSideEffectService.getQueueID(requestId.toString());

		request.setStatus(Request.Status.RUNNING);
		myceliumSideEffectService.workQueue(queueID);

		request = myceliumRequestService.save(request);

		return ResponseEntity.ok().body(request);
	}

}
