package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.model.Vertex;
import ardc.cerium.mycelium.service.MyceliumIndexingService;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import ardc.cerium.mycelium.task.DeleteTask;
import ardc.cerium.mycelium.task.ImportTask;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/services/mycelium")
@Slf4j
@RequiredArgsConstructor
public class MyceliumServiceController {

	private final MyceliumService myceliumService;

	private final MyceliumRequestService myceliumRequestService;

	private final MyceliumSideEffectService myceliumSideEffectService;

	private final MyceliumIndexingService myceliumIndexingService;

    /**
     * Import an XML payload to the {@link MyceliumService}
     * @param json the JSON payload
     * @param sideEffectRequestID the Affected Relationship Request ID
     * @return the {@link ResponseEntity} of a {@link Request}
     */
    @PostMapping("/import-record")
    public ResponseEntity<Request> importRecord(@RequestBody String json, @RequestParam String sideEffectRequestID) {
        log.debug("Received Import Request [sideEffectRequestId={}, payload={}]", sideEffectRequestID, json);

        // create new Request, store the json payload
        RequestDTO dto = new RequestDTO();
        dto.setType(MyceliumRequestService.IMPORT_REQUEST_TYPE);
        Request request = myceliumRequestService.createRequest(dto);
        request.setAttribute(MyceliumSideEffectService.REQUEST_ATTRIBUTE_REQUEST_ID, sideEffectRequestID);

        // store the json payload
        myceliumRequestService.saveToPayloadPath(request, json);
        request.setStatus(Request.Status.ACCEPTED);
        myceliumRequestService.save(request);

        myceliumRequestService.validateImportRequest(request);

        // create the import task and run it immediately
        ImportTask importTask = new ImportTask(request, myceliumService, myceliumSideEffectService);
        importTask.run();

        request = myceliumRequestService.save(request);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/index-record")
    public ResponseEntity<?> indexRecord(@RequestParam String registryObjectId) {
        log.debug("Received Index Request for RegistryObject[id={}]", registryObjectId);
        Vertex from = myceliumService.getVertexFromRegistryObjectId(registryObjectId);
        if(from == null){
            log.error("Vertex with registryObjectId {} doesn't exist",registryObjectId);
            return ResponseEntity.badRequest().body(String.format("Vertex with registryObjectId %s doesn't exist", registryObjectId));
        }
        log.debug("Indexing Vertex[identifier={}]", from.getIdentifier());
        myceliumIndexingService.indexVertex(from);
        log.debug("Index completed Vertex[identifier={}]", from.getIdentifier());

        return ResponseEntity.ok("Done!");
    }

    /**
     * Delete a RegistryObject by ID
     * @param registryObjectId the registryObjectId to be deleted from the Graph
     * @param sideEffectRequestID the requestId of the side effect Request
     * @return a {@link ResponseEntity} of a {@link Request}
     */
    @PostMapping("/delete-record")
    public ResponseEntity<Request> deleteRecord(@RequestParam String registryObjectId,
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
        DeleteTask deleteTask = new DeleteTask(request, myceliumService, myceliumSideEffectService, myceliumIndexingService);
        deleteTask.run();

        request = myceliumRequestService.save(request);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/start-queue-processing")
    public ResponseEntity<?> startQueueProcessing(@Parameter(name = "sideEffectRequestId",
            description = "Request ID of the Side Effect Request") String requestId) {

        log.debug("Received request to process SideEffectQueue Request[id={}]", requestId);

        Request request = myceliumRequestService.findById(requestId);

        // todo confirm and validate request status

        String queueID = myceliumSideEffectService.getQueueID(requestId);
        log.debug("QueueID obtained: {}", queueID);

        request.setStatus(Request.Status.RUNNING);
        myceliumRequestService.save(request);

        // workQueue is an Async method that would set Request to COMPLETED after it has finished
        myceliumSideEffectService.workQueue(queueID, request);

        return ResponseEntity.ok().body(request);
    }

}
