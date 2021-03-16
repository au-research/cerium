package ardc.cerium.core.common.repository;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Embargo;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.util.Helpers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class EmbargoRepositoryTest {

	@Autowired
	TestEntityManager entityManager;

	@Autowired
	EmbargoRepository repository;

	@Autowired
	private RecordRepository recordRepository;

	@Test
	void findById_findByUUID_returnsEmbargo() {
		// given an embargo
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Embargo embargo = TestHelper.mockEmbargo(record);
		repository.save(embargo);

		UUID id = embargo.getId();

		// when findById
		Optional<Embargo> dbFound = repository.findById(id);

		// finds that embargo
		assertThat(dbFound.isPresent()).isTrue();

		Embargo found = dbFound.get();
		assertThat(found).isInstanceOf(Embargo.class);
		assertThat(found.getId()).isEqualTo(embargo.getId());
	}

	@Test
	void findByRecord_returnsEmbargo() {
		// given a record
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Embargo embargo = TestHelper.mockEmbargo(record);
		repository.save(embargo);

		// when findByRecord
		Optional<Embargo> dbFound = repository.findByRecord(record);

		// finds that embargo
		assertThat(dbFound.isPresent()).isTrue();

		Embargo found = dbFound.get();
		assertThat(found).isInstanceOf(Embargo.class);
		assertThat(found.getRecord()).isEqualTo(embargo.getRecord());
	}

	@Test
	void findAllByEmbargoEndLessThanEqual_returnsListEmbargo() {

		Date embargoEnd = Helpers.convertDate("2020-10-27");
		for (int i = 0; i < 110; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.save(record);
			Embargo embargo = TestHelper.mockEmbargo(record);

			embargo.setEmbargoEnd(embargoEnd);
			embargo.setRecord(record);

			repository.save(embargo);
		}
		// when findByRecord
		List<Embargo> embargos = repository.findAllByEmbargoEndLessThanEqual(new Date());

		// finds that embargo
		assertThat(embargos.size()).isEqualTo(110);

	}

	@Test
	void findAllByEmbargoEndLessThanEqual_returnsEmptyListEmbargo() {

		Date today = new Date();
		Date embargoEnd = new Date(today.getTime() + 86400000);

		for (int i = 0; i < 110; i++) {
			Record record = TestHelper.mockRecord();
			recordRepository.save(record);
			Embargo embargo = TestHelper.mockEmbargo(record);

			embargo.setEmbargoEnd(embargoEnd);
			embargo.setRecord(record);

			repository.save(embargo);
		}
		// when findByEmbargoEnd
		List<Embargo> embargos = repository.findAllByEmbargoEndLessThanEqual(new Date());

		// finds all embargo
		assertThat(embargos.size()).isEqualTo(0);

	}

}
