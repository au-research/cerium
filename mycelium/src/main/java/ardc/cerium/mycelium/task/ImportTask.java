package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.service.RequestService;
import ardc.cerium.mycelium.model.RegistryObject;
import ardc.cerium.mycelium.rifcs.RecordState;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

	public ImportTask(String json, Request request, MyceliumService myceliumService) {
		this.json = json;
		this.request = request;
		this.myceliumService = myceliumService;
		this.myceliumSideEffectService = myceliumService.getMyceliumSideEffectService();
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
		Logger requestLogger = LogManager.getLogger(this.getClass().getName());

		RecordState after = null;
		RecordState before = null;
		List<SideEffect> sideEffects = null;
		if (request != null) {
			requestLogger = myceliumService.getMyceliumRequestService().getRequestService().getLoggerFor(request);
		}

		try {
			requestLogger.debug("Ingesting Payload[json={}]", json);
			RegistryObject registryObject = myceliumService.parsePayloadToRegistryObject(json);
			requestLogger.info("Ingesting RegistryObject[id={}]", registryObject.getRegistryObjectId());

			// record before state
			if (request != null) {
				before = myceliumService.getRecordState(registryObject.getRegistryObjectId().toString());
				log.debug("Change Detection, RecordState(before) captured RecordState[{}]", before);
			}


			// ingest the record
			myceliumService.ingestRegistryObject(registryObject);
			myceliumService.getGraphService().reinstateTerminatedNodes();
			log.debug("Ingested registryObject[id={}] payload", registryObject.getRegistryObjectId());
			if (request != null) {
				// record after state
				after = myceliumService.getRecordState(registryObject.getRegistryObjectId().toString());
				log.debug("Change Detection, RecordState(after) captured RecordState[{}]", after);

				// obtain the list of SideEffects from before and after state
				sideEffects = myceliumSideEffectService.detectChanges(before, after);
				log.debug("Change Detection, sideEffect[count={}]", sideEffects.size());
			}


			// add the SideEffects to the queue
			if (request != null) {
				if (!sideEffects.isEmpty()) {
					myceliumSideEffectService.queueSideEffects(request, sideEffects);
					requestLogger.info("{} sideEffects queued for RegistryObject[id={}]", sideEffects.size(), registryObject.getRegistryObjectId());
					requestLogger.debug("SideEffects for RegistryObject[id={}] [queued={}]", registryObject.getRegistryObjectId(), sideEffects);
				} else {
					requestLogger.debug("No sideEffect queued for RegistryObject[id={}], Found:{}", registryObject.getRegistryObjectId(), sideEffects.size());
				}

				// finish the task & request
				request.setMessage("ImportTask successfully completed");
				request.setStatus(Request.Status.COMPLETED);
			}
		}
		catch (Exception e) {
			log.error("Error Importing Reason:{}", e.getMessage());
			try {
				throw e;
			} catch (JsonProcessingException ex) {
				throw new RuntimeException(ex);
			}
		} finally {
			if (request != null) {
				myceliumService.getMyceliumRequestService().getRequestService().closeLoggerFor(request);
			}
		}
	}

}
