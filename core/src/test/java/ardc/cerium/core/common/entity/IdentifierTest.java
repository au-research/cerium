package ardc.cerium.core.common.entity;

import ardc.cerium.core.TestHelper;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.PersistenceException;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IdentifierTest {

	@Autowired
	TestEntityManager entityManager;

	@Test
	void auto_generated_uuid_test() {
		Record record = new Record();
		entityManager.persistAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		entityManager.persistAndFlush(identifier);

		// uuid is generated and is the correct format
		assertThat(identifier.getId()).isNotNull();
		assertThat(identifier.getId()).isInstanceOf(UUID.class);
		assertThat(identifier.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
	}

	@Test
	void throws_exception_when_saving_without_a_record() {
		Identifier identifier = new Identifier();
		Assert.assertThrows(javax.persistence.PersistenceException.class, () -> {
			entityManager.persistAndFlush(identifier);
		});
	}

	@Test
	void an_identifier_must_have_a_date() {
		Identifier identifier = TestHelper.mockIdentifier();
		assertThat(identifier.getCreatedAt()).isInstanceOf(Date.class);
	}

	@Test
	void an_identifier_must_have_a_value() {
		String expected = "10.7531/XXAA998";
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setValue(expected);
		String actual = identifier.getValue();
		assertThat(expected).isEqualTo(actual);
	}

	@Test
	void an_identifier_must_set_dates() {
		Date expected = new Date();
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setUpdatedAt(expected);
		Date actual = identifier.getUpdatedAt();
		assertThat(expected).isEqualTo(actual);
		assertThat(identifier.getUpdatedAt()).isInstanceOf(Date.class);
	}

	@Test
	void an_identifier_must_set_type() {
		Identifier.Type expected = Identifier.Type.IGSN;
		Identifier identifier = TestHelper.mockIdentifier();
		identifier.setType(expected);
		Identifier.Type actual = identifier.getType();
		assertThat(expected).isEqualTo(actual);
	}

	@Test
	@DisplayName("Throws an exception when saved with the same value and type")
	void an_identifier_must_have_unique_type_value() {
		Record record = new Record();
		entityManager.persistAndFlush(record);

		// given an identifier
		Identifier identifier = new Identifier();
		identifier.setRecord(record);
		identifier.setValue("a1");
		identifier.setType(Identifier.Type.DRVS);
		entityManager.persistAndFlush(identifier);

		// when another identifier is persisted with the same value and type (default
		// domain), throws exception
		Assert.assertThrows(PersistenceException.class, () -> {
			Identifier identifier2 = new Identifier();
			identifier2.setRecord(record);
			identifier2.setValue("a1");
			identifier2.setType(Identifier.Type.DRVS);
			entityManager.persistAndFlush(identifier2);
		});
	}

	@Test
	@DisplayName("Allow persisting Identifier with the same value and type but different domain")
	void saving_identifier_different_domain() {
		Record record = new Record();
		entityManager.persistAndFlush(record);

		// given an identifier with domain d1
		Identifier identifier = new Identifier();
		identifier.setRecord(record);
		identifier.setValue("a1");
		identifier.setType(Identifier.Type.DRVS);
		identifier.setDomain("d1");
		entityManager.persistAndFlush(identifier);

		// persist another identifier with same value and type, different domain, should
		// be fine
		Identifier identifier2 = new Identifier();
		identifier2.setRecord(record);
		identifier2.setValue("a1");
		identifier2.setType(Identifier.Type.DRVS);
		identifier.setDomain("d2");
		entityManager.persistAndFlush(identifier2);

	}

}
