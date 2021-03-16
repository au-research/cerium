package ardc.cerium.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

	private String portalUrl;

	private String url;

	private String name;

	private String description;

	private String version;

	private String dataPath;

	private String email;

	public String getPortalUrl() {
		return portalUrl;
	}

	public void setPortalUrl(String portalUrl) {
		this.portalUrl = portalUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDataPath() {
		return dataPath.replaceAll("/$", "");
	}

	public void setDataPath(String dataPath) {
		this.dataPath = dataPath;
	}

	public String getContactEmail() {
		return email;
	}

	public void setContactEmail(String contactEmail) {
		this.email = contactEmail;
	}

}
