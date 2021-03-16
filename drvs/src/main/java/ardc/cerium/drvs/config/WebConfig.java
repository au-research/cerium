package ardc.cerium.drvs.config;

import ardc.cerium.core.common.service.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("drvs")
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	KeycloakService kcService;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**").allowedOrigins("*")
				.allowedMethods(HttpMethod.GET.toString(), HttpMethod.POST.toString(), HttpMethod.PUT.toString(),
						HttpMethod.DELETE.toString(), HttpMethod.OPTIONS.toString())
				.allowedHeaders("*").allowCredentials(true);

		// OpenAPI doc
		registry.addMapping("/v3/api-docs/**").allowedOrigins("*")
				.allowedMethods(HttpMethod.GET.toString(), HttpMethod.POST.toString(), HttpMethod.PUT.toString(),
						HttpMethod.DELETE.toString(), HttpMethod.OPTIONS.toString())
				.allowedHeaders("*").allowCredentials(true);
	}

}
