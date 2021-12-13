package ardc.cerium.mycelium.event;

import ardc.cerium.mycelium.client.RDARegistryClient;
import ardc.cerium.mycelium.model.dto.RDAEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RDAEventListener {

    @Autowired
    RDARegistryClient rdaRegistryClient;

    @EventListener
    public void handleDataSourceUpdated(DataSourceUpdatedEvent event) {
        log.debug("Dispatching event {}", event);
        RDAEventDTO eventDTO = event.toRDAEventDTO();
        rdaRegistryClient.sendWebHookRequest(eventDTO);
    }
}
