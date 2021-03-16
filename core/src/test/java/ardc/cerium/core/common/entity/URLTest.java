package ardc.cerium.core.common.entity;

import ardc.cerium.core.TestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class URLTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void a_url_should_have_auto_generated_uuid() {
		// given a persisted record
		Record record = new Record();
		entityManager.persistAndFlush(record);

		// that has a persisted url
		URL url = TestHelper.mockUrl(record);
		entityManager.persistAndFlush(url);

		// uuid is generated and is the correct format
		assertThat(url.getId()).isNotNull();
		assertThat(url.getId()).isInstanceOf(UUID.class);
		assertThat(url.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void a_url_must_have_a_record() {
		URL url = TestHelper.mockUrl();
		assertThat(url.getRecord()).isInstanceOf(Record.class);
	}

	@Test
	void a_url_must_have_a_date() {
		URL url = TestHelper.mockUrl();
		assertThat(url.getCreatedAt()).isInstanceOf(Date.class);
	}

	@Test
	void a_url_must_have_a_url() {
		String expected = "http://aurl.com";
		URL url = TestHelper.mockUrl();
		url.setUrl(expected);
		String actual = url.getUrl();
		assertThat(expected).isEqualTo(actual);
	}

	@Test
	void a_url_can_be_resolvable() {
		URL url = TestHelper.mockUrl();
		url.setResolvable(true);
		assertThat(url.isResolvable()).isTrue();
	}

	@Test
	void an_identifier_must_set_dates() {
		Date expected = new Date();
		URL url = TestHelper.mockUrl();
		url.setUpdatedAt(expected);
		Date actual_update = url.getUpdatedAt();
		url.setCheckedAt(expected);
		Date actual_checked = url.getCheckedAt();
		assertThat(expected).isEqualTo(actual_update);
		assertThat(url.getUpdatedAt()).isInstanceOf(Date.class);
		assertThat(expected).isEqualTo(actual_checked);
		assertThat(url.getCheckedAt()).isInstanceOf(Date.class);
	}

}
