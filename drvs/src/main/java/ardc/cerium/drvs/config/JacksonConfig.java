package ardc.cerium.drvs.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fix the issue where Jackson doesn't really know the correct types which are contained
 * in AggregatedPage
 *
 * @see <a href=
 * "https://stackoverflow.com/questions/47792915/getting-jackson-parsing-error-while-serializing-aggregatedpage-in-spring-data-el">keyAsNumber</a>
 */
@Configuration
public class JacksonConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer changeKeyAsNumber() {
		return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.mixIn(ParsedStringTerms.ParsedBucket.class,
				MixIn.class);
	}

}

abstract class MixIn {

	@JsonIgnore
	abstract public Number getKeyAsNumber();

}
