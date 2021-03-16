package ardc.cerium.core.common.repository;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class IdentifierRepositoryTest {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	IdentifierRepository repository;

	@Test
	void findById_findByUUID_returnsIdentifier() {
		// given an identifier
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		repository.save(identifier);

		UUID id = identifier.getId();

		// when findById
		Optional<Identifier> dbFound = repository.findById(id);

		// finds that version
		assertThat(dbFound.isPresent()).isTrue();

		Identifier found = dbFound.get();
		assertThat(found).isInstanceOf(Identifier.class);
		assertThat(found.getId()).isEqualTo(identifier.getId());
	}

	@Test
	void existsById_byUUID_returnsTrue() {
		// given an identifier
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		repository.save(identifier);

		UUID id = identifier.getId();

		// when existsById returns true
		assertThat(repository.existsById(id)).isTrue();
	}

	@Test
	void deleteById() {
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Identifier identifier = TestHelper.mockIdentifier(record);
		repository.save(identifier);

		UUID id = identifier.getId();
		repository.deleteById(id);

		assertThat(repository.existsById(id)).isFalse();
	}
}
