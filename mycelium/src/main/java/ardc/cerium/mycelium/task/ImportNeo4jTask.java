package ardc.cerium.mycelium.task;

import ardc.cerium.mycelium.service.MyceliumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportNeo4jTask implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(ImportNeo4jTask.class);

    private String xml;

    private MyceliumService myceliumService;

    public ImportNeo4jTask(String xml, MyceliumService myceliumService) {
        this.xml = xml;
        this.myceliumService = myceliumService;
    }

    @Override
    public void run() {
        try {
            myceliumService.ingest(xml);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
