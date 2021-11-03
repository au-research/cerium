package ardc.cerium.mycelium.task;

import ardc.cerium.mycelium.rifcs.effect.SideEffect;
import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
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

        // todo centralise queueID
        String queueID = String.format("mycelium.queue.effect.datasource.%s", dataSource.getId());
        sideEffects.forEach(sideEffect -> myceliumSideEffectService.addToQueue(queueID, sideEffect));

        // todo work the queue asynchronously
        myceliumSideEffectService.workQueue(queueID);
    }
}
