package ardc.cerium.mycelium.config;

import ardc.cerium.mycelium.client.RDARegistryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MyceliumConfiguration {

	@Value("${rda.registry.url:http://localhost}")
	String rdaRegistryUrl;

	@Bean
	public RDARegistryClient rdaRegistryClient() {
		log.debug("Configured RDARegistryClient with [url:{}]", rdaRegistryUrl);
		RDARegistryClient client = new RDARegistryClient(rdaRegistryUrl);
		return client;
	}

}
