package ardc.cerium.core.common.repository;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class RecordRepositoryTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private RecordRepository repository;

	@Test
	public void injectedComponentsAreNotNull() {
		assertThat(jdbcTemplate).isNotNull();
		assertThat(entityManager).isNotNull();
		assertThat(repository).isNotNull();
	}

	@Test
	public void canFindAll() {
		// given a record
		Record record = new Record();
		repository.save(record);

		// when findsAll, finds 1
		assertThat(repository.findAll()).hasSize(1);

		// adds another record
		Record record2 = new Record();
		repository.save(record2);

		// when findAll, finds 2
		assertThat(repository.findAll()).hasSize(2);
	}

	@Test
	public void repository_can_findById() {
		// given a record
		Record record = new Record();
		repository.save(record);

		UUID id = record.getId();

		// when findById
		Optional<Record> dbFound = repository.findById(id);

		// finds that record
		assertThat(dbFound.isPresent()).isTrue();

		Record found = dbFound.get();
		assertThat(found).isInstanceOf(Record.class);
		assertThat(found).isExactlyInstanceOf(Record.class);
		assertThat(found.getId()).isEqualTo(record.getId());
	}

	@Test
	public void it_can_find_all_records_created_by_a_user() {
		// given 3 records created by Jack
		UUID jackUUID = UUID.randomUUID();
		UUID janeUUID = UUID.randomUUID();
		for (int i = 0; i < 3; i++) {
			Record record = new Record();
			record.setCreatorID(jackUUID);
			repository.save(record);
		}

		// and 2 records created by Jane
		for (int i = 0; i < 2; i++) {
			Record record = new Record();
			record.setCreatorID(janeUUID);
			repository.save(record);
		}

		// Jack has 3 records
		assertThat(repository.findByCreatorID(jackUUID)).hasSize(3);

		// Jane has 2 records
		assertThat(repository.findByCreatorID(janeUUID)).hasSize(2);
	}

	@Test
	public void it_can_find_record_owned_by_a_user() {

		// given Jack and Jane
		UUID jackUUID = UUID.randomUUID();
		UUID janeUUID = UUID.randomUUID();

		// Jack creates 3 records
		for (int i = 0; i < 3; i++) {
			Record record = new Record();
			record.setCreatorID(jackUUID);
			record.setOwnerID(jackUUID);
			record.setOwnerType(Record.OwnerType.User);
			repository.save(record);
		}

		// jane creates 2 record
		for (int i = 0; i < 2; i++) {
			Record record = new Record();
			record.setCreatorID(janeUUID);
			record.setOwnerID(janeUUID);
			record.setOwnerType(Record.OwnerType.User);
			repository.save(record);
		}

		// Jane creates 1 more record, but allocate it to Jack
		Record record = new Record();
		record.setCreatorID(janeUUID);
		record.setOwnerID(jackUUID);
		record.setOwnerType(Record.OwnerType.User);
		repository.save(record);

		// there are 6 records in total
		assertThat(repository.count()).isEqualTo(6);

		// when findOwned for jack, should see 4, 3 he created and 1 he owned
		assertThat(repository.findOwned(jackUUID)).hasSize(4);
	}

	@Test
	public void findAllByVisibleIsTrue_pageable() {
		// given 3 visible records
		for (int i = 0; i < 3; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(true);
			entityManager.persist(record);
		}

		// and 2 private records
		for (int i = 0; i < 2; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(false);
			entityManager.persist(record);
		}

		entityManager.flush();

		Page<Record> firstPage2Records = repository.findAllByVisibleIsTrue(PageRequest.of(0, 2));
		assertThat(firstPage2Records.getContent()).hasSize(2);

		Page<Record> allVisibleRecords = repository.findAllByVisibleIsTrue(PageRequest.of(0, 100));
		assertThat(allVisibleRecords.getContent()).hasSize(3);
	}

	@Test
	public void specificationsTest() {
		// given 3 visible records
		for (int i = 0; i < 3; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(true);
			entityManager.persist(record);
		}

		// and 2 private records
		for (int i = 0; i < 2; i++) {
			Record record = TestHelper.mockRecord();
			record.setVisible(false);
			entityManager.persist(record);
		}

		// found 3 visible records
		RecordSpecification visibleSpec = new RecordSpecification();
		visibleSpec.add(new SearchCriteria("visible", true, SearchOperation.EQUAL));
		List<Record> visibleRecord = repository.findAll(visibleSpec);
		assertThat(visibleRecord).hasSize(3);

		// found 2 private records
		RecordSpecification privateSpecs = new RecordSpecification();
		privateSpecs.add(new SearchCriteria("visible", false, SearchOperation.EQUAL));
		List<Record> privateRecords = repository.findAll(privateSpecs);
		assertThat(privateRecords).hasSize(2);
	}

}
