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

import static com.mysql.cj.util.StringUtils.getBytes;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class VersionTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void a_version_should_have_auto_generated_uuid() {
		Record record = new Record();
		entityManager.persistAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		entityManager.persistAndFlush(version);

		// uuid is generated and is the correct format
		assertThat(version.getId()).isNotNull();
		assertThat(version.getId()).isInstanceOf(UUID.class);
		assertThat(version.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void throws_exception_when_saving_without_a_record() {
		Version version = new Version();
		Assert.assertThrows(javax.persistence.PersistenceException.class, () -> {
			entityManager.persistAndFlush(version);
		});
	}

	@Test
	void a_version_must_have_a_record() {
		Version version = TestHelper.mockVersion();
		assertThat(version.getRecord()).isInstanceOf(Record.class);
	}

	@Test
	void a_version_must_have_dates() {
		Version version = TestHelper.mockVersion();
		assertThat(version.getCreatedAt()).isInstanceOf(Date.class);
		assertThat(version.getEndedAt()).isNull();
	}

	@Test
	void a_version_must_have_creator() {
		Version version = TestHelper.mockVersion();
		assertThat(version.getCreatorID()).isInstanceOf(UUID.class);
	}

	@Test
	void a_version_can_have_content() {
		String expected = "Some content";
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		entityManager.clear();
		Version version = new Version();

		version.setRecord(record);
		version.setContent(getBytes(expected));
		entityManager.persistAndFlush(version);

		String actual = new String(version.getContent());
		assertThat(expected).isEqualTo(actual);
	}

}