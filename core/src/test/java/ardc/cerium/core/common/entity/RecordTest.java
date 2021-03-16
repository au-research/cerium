package ardc.cerium.core.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class RecordTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void a_record_have_auto_generated_uuid() {

		// given a saved record
		Record record = new Record();
		entityManager.persistAndFlush(record);

		// uuid is generated and is the correct format
		assertThat(record.getId()).isNotNull();
		assertThat(record.getId()).isInstanceOf(UUID.class);
		assertThat(record.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void a_record_has_visible() {
		Record record = new Record();
		record.setVisible(false);
		entityManager.persistAndFlush(record);
		assertThat(record.isVisible()).isFalse();

		Record record2 = new Record();
		entityManager.persistAndFlush(record);
		assertThat(record2.isVisible()).isTrue();
	}

}