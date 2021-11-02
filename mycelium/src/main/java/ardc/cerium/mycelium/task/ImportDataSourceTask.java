package ardc.cerium.mycelium.task;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;
import lombok.extern.slf4j.Slf4j;

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
        // todo SideEffect
        // todo logging
        // todo contact RDA Registry data source logs
        if (myceliumService.getDataSourceById(dataSource.getId()) != null) {
            log.debug("Deleting DataSource[id={}]", dataSource.getId());
            myceliumService.deleteDataSourceById(dataSource.getId());
        }

        log.debug("Importing DataSource[id={}]", dataSource.getId());
        myceliumService.importDataSource(dataSource);
    }
}
