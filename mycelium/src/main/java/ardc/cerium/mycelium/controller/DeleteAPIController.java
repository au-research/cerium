package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.task.DeleteTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class DeleteAPIController {

    private final MyceliumService myceliumService;

    private final MyceliumRequestService myceliumRequestService;

    public DeleteAPIController(MyceliumService myceliumService, MyceliumRequestService myceliumRequestService) {
        this.myceliumService = myceliumService;
        this.myceliumRequestService = myceliumRequestService;
    }

    /**
     * Import an XML payload to the {@link MyceliumService}
     * @param registryObjectId the registryObjectId to be deleted from the Graph
     * @return a {@link ResponseEntity} of a {@link Request}
     */
    @PostMapping("/delete-record")
    public ResponseEntity<Request> deleteHandler(@RequestParam String registryObjectId) {

        // create and save the request
        RequestDTO dto = new RequestDTO();
        dto.setType(MyceliumRequestService.DELETE_REQUEST_TYPE);
        Request request = myceliumRequestService.createRequest(dto);
        request.setStatus(Request.Status.ACCEPTED);
        request.setAttribute(Attribute.RECORD_ID, registryObjectId);
        myceliumRequestService.save(request);

        DeleteTask deleteTask = new DeleteTask(request, myceliumService);
        deleteTask.run();

        request.setStatus(Request.Status.COMPLETED);
        myceliumRequestService.save(request);
        return ResponseEntity.ok(request);
    }

}