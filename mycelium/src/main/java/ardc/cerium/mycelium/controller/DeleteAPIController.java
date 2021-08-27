package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import ardc.cerium.mycelium.task.DeleteTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Import Controller for Mycelium service
 *
 * @author Minh Duc Nguyen
 */
@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class DeleteAPIController {

	private final MyceliumService myceliumService;

	private final MyceliumRequestService myceliumRequestService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	public DeleteAPIController(MyceliumService myceliumService, MyceliumRequestService myceliumRequestService,
			MyceliumSideEffectService myceliumSideEffectService) {
		this.myceliumService = myceliumService;
		this.myceliumRequestService = myceliumRequestService;
		this.myceliumSideEffectService = myceliumSideEffectService;
	}

	/**
	 * Delete a RegistryObject by ID
	 * @param registryObjectId the registryObjectId to be deleted from the Graph
	 * @param sideEffectRequestID the requestId of the side effect Request
	 * @return a {@link ResponseEntity} of a {@link Request}
	 */
	@PostMapping("/delete-record")
	public ResponseEntity<Request> destroy(@RequestParam String registryObjectId,
			@RequestParam String sideEffectRequestID) {

		// create and save the request
		RequestDTO dto = new RequestDTO();
		dto.setType(MyceliumRequestService.DELETE_REQUEST_TYPE);
		Request request = myceliumRequestService.createRequest(dto);
		request.setStatus(Request.Status.ACCEPTED);
		request.setAttribute(Attribute.RECORD_ID, registryObjectId);
		request.setAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID, sideEffectRequestID);
		request = myceliumRequestService.save(request);

		// run the DeleteTask
		DeleteTask deleteTask = new DeleteTask(request, myceliumService, myceliumSideEffectService);
		deleteTask.run();

		request = myceliumRequestService.save(request);
		return ResponseEntity.ok(request);
	}

}