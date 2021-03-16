package ardc.cerium.core.common.repository;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class URLRepositoryTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private URLRepository urlRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Test
	public void injectedComponentsAreNotNull() {
		assertThat(jdbcTemplate).isNotNull();
		assertThat(entityManager).isNotNull();
		assertThat(urlRepository).isNotNull();
	}

	@Test
	public void repository_can_find_by_id() {
		// given a url
		Record record = TestHelper.mockRecord();
		recordRepository.save(record);
		URL url = TestHelper.mockUrl(record);
		urlRepository.save(url);

		UUID id = url.getId();

		// when findById
		Optional<URL> dbFound = urlRepository.findById(id);

		// finds that url
		assertThat(dbFound.isPresent()).isTrue();

		URL found = dbFound.get();
		assertThat(found).isInstanceOf(URL.class);
		assertThat(found.getId()).isEqualTo(url.getId());
	}

}
