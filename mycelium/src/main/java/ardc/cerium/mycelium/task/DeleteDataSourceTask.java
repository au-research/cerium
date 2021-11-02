package ardc.cerium.mycelium.task;

import ardc.cerium.mycelium.rifcs.model.datasource.DataSource;
import ardc.cerium.mycelium.service.MyceliumService;

public class DeleteDataSourceTask implements Runnable{

    private final MyceliumService myceliumService;

    private final String dataSourceId;

    public DeleteDataSourceTask(MyceliumService myceliumService, String dataSourceId) {
        this.myceliumService = myceliumService;
        this.dataSourceId = dataSourceId;
    }

    @Override
    public void run() {
        // todo SideEffect
        // todo logging
        // todo contact RDA Registry data source logs
        myceliumService.deleteDataSourceById(dataSourceId);
    }
}
