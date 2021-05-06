package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.task.ImportTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Import Controller for Mycelium service
 *
 * @author Minh Duc Nguyen
 */
@RestController
@RequestMapping(value = "/api/services/")
@Slf4j
public class MyceliumImportController {

	// todo ? import-bulk to import multiple XML documents at the same time, wrapped?
	// todo ? import-remote to import from a remote endpoint, ie RDA?
	// todo ? /index -> IndexController?

	private final MyceliumService myceliumService;

	public MyceliumImportController(MyceliumService myceliumService) {
		this.myceliumService = myceliumService;
	}

	/**
	 * Import an XML payload to the {@link MyceliumService}
	 * @param xml the XML payload
	 * @return something
	 */
	@PostMapping("/import")
	public ResponseEntity<Request> importHandler(@RequestBody String xml) {
		// create new Request, store the xml
		Request request = myceliumService.createImportRequest(xml);
		request.setStatus(Request.Status.ACCEPTED);

		ImportTask importTask = new ImportTask(request, myceliumService);
		importTask.run();

		myceliumService.save(request);
		return ResponseEntity.ok(request);
	}

	/**
	 * Import from a local directory path. Currently used for load testing purposes
	 * @param directory the path to the local directory, contains a flat list of XML files
	 * to be imported
	 * @return something
	 * @throws IOException when the directory is not readable
	 */
	@PostMapping("/import-local")
	public ResponseEntity<?> importLocal(@RequestBody String directory) throws IOException {
		File directoryPath = new File(directory);
		File[] filesList = directoryPath.listFiles();
		assert filesList != null;
		log.info("Obtaining {} files from path {}", filesList.length, directory);

		// multi thread this
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
		for (File file : filesList) {
			String xml = FileUtils.readFileToString(file, "UTF-8");
			threadPoolExecutor.execute(new ImportTask(xml, myceliumService));
		}

		// todo determine return type
		return ResponseEntity.ok("Done");
	}

}
