package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.task.ImportTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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

	@Value("${app.ip-white-list:0:0:0:0:0:0:0:1,127.0.0.1,130.56.111.120}")
	String ipWhiteList;

	public ImportAPIController(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
	}

	/**
	 * Import an XML payload to the {@link MyceliumService}
	 * @param json the JSON payload
	 * @param sRequest the current {@link HttpServletRequest}
	 * @return the {@link ResponseEntity} of a {@link Request}
	 */
	@PostMapping("/import-record")
	public ResponseEntity<Request> importHandler(@RequestBody String json, HttpServletRequest sRequest) {
		// poor man's security until we decide the way we proceed !
		// but most likely only allow requests from localhost is a good start
		if (!ipWhiteList.contains(sRequest.getRemoteAddr())) {
			throw new ForbiddenOperationException(String.format("Ip %s, not authorised", sRequest.getRemoteAddr()));
		}
		// create new Request, store the json payload
		Request request = myceliumService.createImportRequest(json);
		request.setStatus(Request.Status.ACCEPTED);
		myceliumService.validateRequest(request);

		ImportTask importTask = new ImportTask(request, myceliumService);
		importTask.run();

		request.setStatus(Request.Status.COMPLETED);
		myceliumService.save(request);
		return ResponseEntity.ok(request);
	}

}
