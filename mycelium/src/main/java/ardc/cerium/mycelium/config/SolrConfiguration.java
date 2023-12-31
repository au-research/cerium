package ardc.cerium.mycelium.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.convert.SolrJConverter;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@Configuration
@EnableSolrRepositories(basePackages = "ardc.cerium.mycelium.repository")
public class SolrConfiguration {

	@Value("${spring.data.solr.host:http://localhost:8983/solr}")
	String solrUrl;

	@Bean
	public SolrClient solrClient() {
		HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
		builder.withBaseSolrUrl(solrUrl);
		return builder.build();
	}

	@Bean
	public SolrConverter solrAConverter() {
		return new SolrJConverter();
	}

	@Bean
	public SolrTemplate solrTemplate(SolrClient client) {
		return new SolrTemplate(client);
	}

}
