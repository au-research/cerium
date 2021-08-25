package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ImportTask implements Runnable {

	private final MyceliumService myceliumService;

	private String json;

	private final Request request;

	/**
	 * Instantiation with an Import {@link Request}. The XML will be obtained from the
	 * Request's PAYLOAD_PATH
	 * @param request the {@link Request} to run on
	 * @param myceliumService the {@link MyceliumService}
	 */
	public ImportTask(Request request, MyceliumService myceliumService) {
		this.request = request;
		this.myceliumService = myceliumService;
		parseRequest(request);
	}

	public void parseRequest(Request request) {
		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		try {
			this.json = Files.readString(Paths.get(payloadPath));
		}
		catch (IOException e) {
			log.error("Failed loading payload from {} {} , ", payloadPath, e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			myceliumService.ingest(json, request);
			// todo update Request status and/or logging
		}
		catch (Exception e) {
			log.error("Error Ingesting RequestID:{} Reason: {}", request.getId(), e.getMessage());
			e.printStackTrace();
		}
	}

}
