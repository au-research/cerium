package ardc.cerium.igsn.controller;

import ardc.cerium.igsn.KeycloakIntegrationTest;
import ardc.cerium.igsn.TestHelper;
import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.RequestService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@EnabledIf(expression = "${app.igsn.enabled}", reason = "Disable test if IGSN is not enabled", loadContext = true)
class IGSNRequestControllerIT extends KeycloakIntegrationTest {

    @MockBean
    KeycloakService kcService;

    @MockBean
    RequestService requestService;

    @Test
    public void check_is_request_is_completed_or_failed(){
        Request request = TestHelper.mockRequest();
        when(requestService.findOwnedById(any(), any())).thenReturn(request);
        this.webTestClient.put().uri("api/resources/igsn-requests/434343434?status=RESTART")
                .header("Authorization", getBasicAuthenticationHeader(username, password))
                .header("Content-Type", "application/xml").exchange().expectStatus().isForbidden();
    }

}