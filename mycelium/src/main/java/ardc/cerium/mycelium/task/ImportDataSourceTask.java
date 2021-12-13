package ardc.cerium.mycelium.task;

import ardc.cerium.core.common.dto.RequestDTO;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.mycelium.event.DataSourceUpdatedEvent;
import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumRequestService;
import ardc.cerium.mycelium.service.MyceliumService;
import ardc.cerium.mycelium.service.MyceliumSideEffectService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImportDataSourceTask implements Runnable{

    private final MyceliumService myceliumService;

    private final DataSource dataSource;

    public ImportDataSourceTask(MyceliumService myceliumService, DataSource dataSource) {
        this.myceliumService = myceliumService;
        this.dataSource = dataSource;
    }

    @Override
    public void run() {

        // capture the before state (for side effect execution)
        DataSource before = myceliumService.getDataSourceById(dataSource.getId());

        // todo logging
        // todo contact RDA Registry data source logs
        if (myceliumService.getDataSourceById(dataSource.getId()) != null) {
            log.debug("Deleting DataSource[id={}]", dataSource.getId());
            myceliumService.deleteDataSourceById(dataSource.getId());
        }

        // perform the import
        log.debug("Importing DataSource[id={}]", dataSource.getId());
        myceliumService.importDataSource(dataSource);

        // capture the after state (for side effect execution)
        DataSource after = myceliumService.getDataSourceById(dataSource.getId());

        MyceliumSideEffectService myceliumSideEffectService = myceliumService.getMyceliumSideEffectService();
        List<SideEffect> sideEffects = myceliumSideEffectService.detectChanges(before, after);

        // no need to do anything if there's no sideEffect
        if (sideEffects.size() == 0) {
            return;
        }

        // create a new Request to track progress and store queue
        RequestDTO dto = new RequestDTO();
        dto.setType(MyceliumRequestService.AFFECTED_REL_REQUEST_TYPE);
        Request request = myceliumService.createRequest(dto);

        // queue the sideEffects
        String queueID = myceliumSideEffectService.getQueueID(request.getId().toString());
        sideEffects.forEach(sideEffect -> myceliumSideEffectService.addToQueue(queueID, sideEffect));

		// send an event notifying RDA that we're starting the queue
		myceliumService.publishEvent(new DataSourceUpdatedEvent(this, dataSource.getId(),
				"Affected record processing started. \nRequestID: " + request.getId().toString()));

        // work the queue asynchronously, and send finished message once done
        myceliumSideEffectService.workQueueAsync(queueID, request, new DataSourceUpdatedEvent(this,
				dataSource.getId(), "Affected record processing completed. \nRequestID: "+request.getId().toString()));
    }
}
