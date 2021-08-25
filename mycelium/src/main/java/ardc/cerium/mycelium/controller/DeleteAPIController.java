package ardc.cerium.mycelium.controller;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.exception.ForbiddenOperationException;
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

    @Value("${app.ip-white-list:0:0:0:0:0:0:0:1,127.0.0.1,130.56.111.120}")
    String ipWhiteList;

    // todo ? import-bulk to import multiple XML documents at the same time, wrapped?
    // todo ? import-remote to import from a remote endpoint, ie RDA?
    // todo ? /index -> IndexController?

    private final MyceliumService myceliumService;

    public DeleteAPIController(MyceliumService myceliumService) {
        this.myceliumService = myceliumService;
    }

    /**
     * Import an XML payload to the {@link MyceliumService}
     * @param registryObjectId the registryObjectId to be deleted from the Graph
     * @param sRequest the {@link HttpServletRequest} server Request
     * @return a {@link ResponseEntity} of a {@link Request}
     */
    @PostMapping("/delete-record")
    public ResponseEntity<Request> deleteHandler(@RequestParam String registryObjectId, HttpServletRequest sRequest) {
        // poor man's security until we decide the way we proceed !
        // but most likely only allow requests from localhost is a good start
        if(!ipWhiteList.contains(sRequest.getRemoteAddr())){
            throw new ForbiddenOperationException(String.format("Ip %s, not authorised",sRequest.getRemoteAddr()));
        }
        // create new Request, store thejson payload
        Request request = myceliumService.createDeleteRequest(registryObjectId);
        request.setStatus(Request.Status.ACCEPTED);

        DeleteTask deleteTask = new DeleteTask(request, myceliumService);
        deleteTask.run();

        request.setStatus(Request.Status.COMPLETED);
        myceliumService.save(request);
        return ResponseEntity.ok(request);
    }

}