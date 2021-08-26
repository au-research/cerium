package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import ardc.cerium.mycelium.task.ImportTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Import Controller for Mycelium service
 *
 * @author Minh Duc Nguyen
 */
@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
public class ImportAPIController {

	private final MyceliumService myceliumService;

	private final MyceliumRequestService myceliumRequestService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	public ImportAPIController(MyceliumService myceliumService, MyceliumRequestService myceliumRequestService,
			MyceliumSideEffectService myceliumSideEffectService) {
		this.myceliumService = myceliumService;
		this.myceliumRequestService = myceliumRequestService;
		this.myceliumSideEffectService = myceliumSideEffectService;
	}

	/**
	 * Import an XML payload to the {@link MyceliumService}
	 * @param json the JSON payload
	 * @param sideEffectRequestID the Affected Relationship Request ID
	 * @return the {@link ResponseEntity} of a {@link Request}
	 */
	@PostMapping("/import-record")
	public ResponseEntity<Request> importHandler(@RequestBody String json, @RequestParam String sideEffectRequestID) {

		// create new Request, store the json payload
		RequestDTO dto = new RequestDTO();
		dto.setType(MyceliumRequestService.IMPORT_REQUEST_TYPE);
		Request request = myceliumRequestService.createRequest(dto);
		request.setAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID, sideEffectRequestID);

		// store the json payload
		myceliumRequestService.saveToPayloadPath(request, json);
		request.setStatus(Request.Status.ACCEPTED);
		myceliumRequestService.save(request);

		myceliumService.validateRequest(request);

		// create the import task and run it immediately
		ImportTask importTask = new ImportTask(request, myceliumService, myceliumSideEffectService);
		importTask.run();

		request = myceliumRequestService.save(request);
		return ResponseEntity.ok(request);
	}

}
