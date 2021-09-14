package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class ImportTask implements Runnable {

	private final MyceliumService myceliumService;

	private final MyceliumSideEffectService myceliumSideEffectService;

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
		this.myceliumSideEffectService = myceliumService.getMyceliumSideEffectService();
		parseRequest(request);
	}

	public void parseRequest(Request request) {
		String payloadPath = request.getAttribute(Attribute.PAYLOAD_PATH);
		try {
			this.json = Files.readString(Paths.get(payloadPath));
		}
		catch (IOException e) {
			log.error("Failed parsing payload[path={}] Reason: {}", payloadPath, e.getMessage());
		}
	}

	@Override
	public void run() {

		try {
			RegistryObject registryObject = myceliumService.parsePayloadToRegistryObject(json);

			RecordState before = myceliumService.getRecordState(registryObject.getRegistryObjectId().toString());
			log.debug("Change Detection, RecordState(before) captured RecordState[{}]", before);

			myceliumService.ingestRegistryObject(registryObject);
			myceliumService.getGraphService().reinstateTerminatedNodes();
			log.debug("Ingested registryObject[id={}] payload", registryObject.getRegistryObjectId());

			RecordState after = myceliumService.getRecordState(registryObject.getRegistryObjectId().toString());
			log.debug("Change Detection, RecordState(after) captured RecordState[{}]", after);

			// obtain the list of SideEffects
			List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
			log.debug("Change Detection, sideEffect[count={}]", sideEffects.size());

			// add the SideEffects to the queue
			myceliumSideEffectService.queueSideEffects(request, sideEffects);

			request.setMessage("ImportTask successfully completed");
			request.setStatus(Request.Status.COMPLETED);
		}
		catch (Exception e) {
			log.error("Error Ingesting RequestID:{} Reason:{}", request.getId(), e.getMessage());
			e.printStackTrace();
		}
	}

}
