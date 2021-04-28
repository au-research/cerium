package ardc.cerium.mycelium.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.solr.SolrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@Configuration
@EnableSolrRepositories(value = "ardc.cerium.mycelium.repository")
public class SolrConfiguration {

	@Autowired
	SolrProperties solrProperties;

	@Bean
	public SolrClient solrClient() {
		HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
		builder.withBaseSolrUrl(solrProperties.getHost());
		return builder.build();
	}

	@Bean
	public SolrTemplate solrTemplate(SolrClient client) {
		return new SolrTemplate(client);
	}

}
