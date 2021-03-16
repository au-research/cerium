package ardc.cerium.core.common.entity;

import ardc.cerium.core.TestHelper;
import org.junit.Assert;
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
public class EmbargoTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void auto_generated_uuid_test() {
		Record record = new Record();
		entityManager.persistAndFlush(record);
		Embargo embargo = TestHelper.mockEmbargo(record);
		entityManager.persistAndFlush(embargo);

		assertThat(embargo.getId()).isNotNull();
		assertThat(embargo.getId()).isInstanceOf(UUID.class);
		assertThat(embargo.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void throws_exception_when_saving_without_a_record() {
		Embargo embargo = new Embargo();
		Assert.assertThrows(javax.persistence.PersistenceException.class, () -> {
			entityManager.persistAndFlush(embargo);
		});
	}

	@Test
	void an_embargo_must_have_a_date() {
		Embargo embargo = TestHelper.mockEmbargo();
		assertThat(embargo.getEmbargoEnd()).isInstanceOf(Date.class);
	}

	@Test
	void an_embargo_must_set_date() {
		Date expected = new Date();
		Embargo embargo = TestHelper.mockEmbargo();
		embargo.setEmbargoEnd(expected);
		Date actual = embargo.getEmbargoEnd();
		assertThat(expected).isEqualTo(actual);
		assertThat(embargo.getEmbargoEnd()).isInstanceOf(Date.class);
	}

	@Test
	void an_embargo_with_supplied_uuid() {
		Embargo embargo = new Embargo(UUID.randomUUID());
		assertThat(embargo.getId()).isInstanceOf(UUID.class);
	}

	@Test
	void an_embargo_must_get_record() {
		Record record = new Record();
		entityManager.persistAndFlush(record);
		Embargo embargo = TestHelper.mockEmbargo(record);
		entityManager.persistAndFlush(embargo);

		assertThat(embargo.getRecord()).isInstanceOf(Record.class);

	}

}
