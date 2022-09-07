package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Slf4j
public class DeleteTask implements Runnable {

	private final MyceliumService myceliumService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	private final Request request;

	private final String registryObjectId;

	/**
	 * Instantiation with an Import {@link Request}. The XML will be obtained from the
	 * Request's PAYLOAD_PATH
	 * @param request the {@link Request} to run on
	 * @param myceliumService the {@link MyceliumService}
	 */
	public DeleteTask(Request request, MyceliumService myceliumService) {
		this.request = request;
		this.myceliumService = myceliumService;
		this.myceliumSideEffectService = myceliumService.getMyceliumSideEffectService();
		registryObjectId = request.getAttribute(Attribute.RECORD_ID);
	}

	public DeleteTask(String registryObjectId, Request request, MyceliumService myceliumService) {
		this.registryObjectId = registryObjectId;
		this.request = request;
		this.myceliumService = myceliumService;
		this.myceliumSideEffectService = myceliumService.getMyceliumSideEffectService();
	}

	@Override
	public void run() {
		Logger requestLogger = LogManager.getLogger(this.getClass().getName());
		if (request != null) {
			requestLogger = myceliumService.getMyceliumRequestService().getRequestService().getLoggerFor(request);
		}

		try {
			Vertex vertex = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
			if (vertex == null) {
				log.error("Vertex with registryObjectId {} doesn't exist", registryObjectId);
				return;
			}
			log.debug("Started deleting registryObject[id={}]", registryObjectId);

			RecordState before = null;
			if (request != null) {
				before = myceliumService.getRecordState(registryObjectId);
				log.debug("Change Detection, RecordState(before) captured RecordState[{}]", before);
			}

			myceliumService.deleteRecord(registryObjectId);
			myceliumService.getGraphService().setRegistryObjectKeyNodeTerminated();

			requestLogger.info("Deleted RegistryObject[id={}]", registryObjectId);

			if (request != null) {
				List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, null);
				log.debug("Change Detection, sideEffect[count={}]", sideEffects.size());

				// add the SideEffects to the queue
				if (!sideEffects.isEmpty()) {
					myceliumSideEffectService.queueSideEffects(request, sideEffects);
					requestLogger.info("{} sideEffects queued for RegistryObject[id={}]", sideEffects.size(), registryObjectId);
					requestLogger.debug("SideEffects for RegistryObject[id={}] [queued={}]", registryObjectId, sideEffects);
				} else {
					requestLogger.info("No sideEffect found for RegistryObject[id={}]", registryObjectId);
				}
			}

		}
		catch (Exception e) {
			log.error("Error in DeleteTask Request[id={}] Reason: {}", request.getId(), e.getMessage());
			e.printStackTrace();
		}
	}

}
