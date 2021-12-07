package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.Logger;

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
		Logger requestLogger = myceliumService.getMyceliumRequestService().getRequestService().getLoggerFor(request);

		try {
			log.debug("Started deleting registryObject[id={}]", registryObjectId);

			RecordState before = myceliumService.getRecordState(registryObjectId);
			log.debug("Change Detection, RecordState(before) captured RecordState[{}]", before);

			myceliumService.deleteRecord(registryObjectId);
			myceliumService.getGraphService().setRegistryObjectKeyNodeTerminated();
			requestLogger.info("Deleted RegistryObject[id={}]", registryObjectId);

			RecordState after = myceliumService.getRecordState(registryObjectId);
			log.debug("Change Detection, RecordState(after) captured RecordState[{}]", before);

			List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
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
		catch (Exception e) {
			log.error("Error in DeleteTask Request[id={}] Reason: {}", request.getId(), e.getMessage());
			e.printStackTrace();
		}
	}

}
