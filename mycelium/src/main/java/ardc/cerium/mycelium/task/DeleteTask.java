package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteTask implements Runnable {

    private final MyceliumService myceliumService;

    private String json;

    private final Request request;

    /**
     * @param request the {@link Request} to run delete of the Record_id set by the controller
     * @param myceliumService the {@link MyceliumService}
     */
    public DeleteTask(Request request, MyceliumService myceliumService) {
        this.request = request;
        this.myceliumService = myceliumService;
    }


    @Override
    public void run() {
        try {
            myceliumService.deleteRecord(this.request.getAttribute(Attribute.RECORD_ID), request);
            // todo update Request status and/or logging
        }
        catch (Exception e) {
            log.error("Error Deleting Record in RequestID:{} Reason: {}", request.getId(), e.getMessage());
            e.printStackTrace();
        }
    }

}
