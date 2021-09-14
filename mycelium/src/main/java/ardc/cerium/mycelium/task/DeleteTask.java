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

import java.util.List;

@Slf4j
public class DeleteTask implements Runnable {

	private final MyceliumService myceliumService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	private final Request request;

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
	}

	@Override
	public void run() {
		try {
			String registryObjectId = request.getAttribute(Attribute.RECORD_ID);
			log.debug("Started deleting registryObject[id={}]", registryObjectId);

			RecordState before = myceliumService.getRecordState(registryObjectId);
			log.debug("Change Detection, RecordState(before) captured RecordState[{}]", before);

			myceliumService.deleteRecord(registryObjectId);
			myceliumService.getGraphService().setRegistryObjectKeyNodeTerminated();

			RecordState after = myceliumService.getRecordState(registryObjectId);
			log.debug("Change Detection, RecordState(after) captured RecordState[{}]", before);

			List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);
			log.debug("Change Detection, sideEffect[count={}]", sideEffects.size());

			// add the SideEffects to the queue
			myceliumSideEffectService.queueSideEffects(request, sideEffects);

			request.setMessage("DeleteTask Finished Successfully");
			request.setStatus(Request.Status.COMPLETED);
		}
		catch (Exception e) {
			log.error("Error in DeleteTask Request[id={}] Reason: {}", request.getId(), e.getMessage());
			e.printStackTrace();
		}
	}

}
