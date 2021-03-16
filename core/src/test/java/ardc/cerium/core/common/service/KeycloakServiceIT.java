package ardc.cerium.core.common.service;

import ardc.cerium.core.KeycloakIntegrationTest;
import ardc.cerium.core.common.model.DataCenter;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class KeycloakServiceIT extends KeycloakIntegrationTest {

    @Autowired
    private KeycloakService keycloakService;

    @Test
    void getDataCenterByUUID() throws Exception {
        DataCenter dataCenter = keycloakService.getDataCenterByUUID(UUID.fromString("bfcefcfd-dc5c-4083-a554-85888334f353"));
        Assert.assertEquals("TestUserGroup1", dataCenter.getName());
    }

}