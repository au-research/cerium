package ardc.cerium.oai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@EnabledIf(expression = "${keycloak.enabled}", reason = "Disable test if keycloak is not enabled", loadContext = true)
public abstract class KeycloakIntegrationTest extends WebIntegrationTest {

	@Value("${test.kc.user.id}")
	public String userID;

	@Value("${test.kc.user.username}")
	public String username;

	@Value("${test.kc.user.password}")
	public String password;

	@Value("${test.kc.user.rsid}")
	public String resourceID;

	@Value("${test.kc.admin.username}")
	public String adminUsername;

	@Value("${test.kc.admin.password}")
	public String adminPassword;

}
