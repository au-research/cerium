package ardc.cerium.core.common.repository;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class VersionRepositoryTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private VersionRepository versionRepository;

	@Autowired
	private RecordRepository recordRepository;

	@Test
	public void injectedComponentsAreNotNull() {
		assertThat(jdbcTemplate).isNotNull();
		assertThat(entityManager).isNotNull();
		assertThat(versionRepository).isNotNull();
	}

	@Test
	public void repository_can_find_by_id() {
		// given a version
		Record record = TestHelper.mockRecord();
		recordRepository.save(record);
		Version version = TestHelper.mockVersion(record);
		versionRepository.save(version);

		UUID id = version.getId();

		// when findById
		Optional<Version> dbFound = versionRepository.findById(id);

		// finds that version
		assertThat(dbFound.isPresent()).isTrue();

		Version found = dbFound.get();
		assertThat(found).isInstanceOf(Version.class);
		assertThat(found.getId()).isEqualTo(version.getId());
	}

	@Test
	public void can_find_existence_by_id() {
		// given a version
		Record record = TestHelper.mockRecord();
		recordRepository.save(record);
		Version version = TestHelper.mockVersion(record);
		versionRepository.save(version);

		UUID id = version.getId();

		assertThat(versionRepository.existsById(UUID.randomUUID())).isFalse();
		assertThat(versionRepository.existsById(id)).isTrue();
	}

	@Test
	public void existsByHash() {
		// given a version with a hash
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		String hash = DigestUtils.sha1Hex(version.getContent());
		version.setHash(hash);
		entityManager.persistAndFlush(version);

		// existsByHash is correct
		assertThat(versionRepository.existsByHash(hash)).isTrue();
		assertThat(versionRepository.existsByHash(DigestUtils.sha1Hex("some random string"))).isFalse();
	}

	@Test
	public void existsBySchemaAndHash() {
		// given a version with a hash
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		String schema = version.getSchema();
		String hash = DigestUtils.sha1Hex(version.getContent());
		version.setHash(hash);
		entityManager.persistAndFlush(version);

		// existsBySchemaAndHash is correct
		assertThat(versionRepository.existsBySchemaAndHash(schema, hash)).isTrue();
		assertThat(versionRepository.existsBySchemaAndHash(schema, DigestUtils.sha1Hex("some random string")))
				.isFalse();
	}

	@Test
	public void existsBySchemaAndHashAndCurrent() {
		// given a version with a hash
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		String schema = version.getSchema();
		String hash = DigestUtils.sha1Hex(version.getContent());
		version.setHash(hash);
		entityManager.persistAndFlush(version);

		assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, hash, true)).isTrue();
		assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, hash, false)).isFalse();
		assertThat(versionRepository.existsBySchemaAndHashAndCurrent(schema, DigestUtils.sha1Hex("some random string"),
				true)).isFalse();
	}

	@Test
	public void testSpecifications() {
		// given a record
		Record record = TestHelper.mockRecord();
		entityManager.persistAndFlush(record);

		// with a current version of schema ardc.cerium.core.igsn-descriptive-v1
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		entityManager.persistAndFlush(version);

		// and 5 superseded version of schema ardc.cerium.core.igsn-descriptive-v1
		for (int i = 0; i < 5; i++) {
			Version superseded = TestHelper.mockVersion(record);
			superseded.setCurrent(false);
			entityManager.persist(superseded);
		}
		entityManager.flush();

		// find all version by record
		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("record", record, SearchOperation.EQUAL));
		Page<Version> allVersions = versionRepository.findAll(specs, PageRequest.of(0, 10));
		assertThat(allVersions).hasSize(6);

		// find all version by record and current
		VersionSpecification recordAndCurrentSpec = new VersionSpecification();
		recordAndCurrentSpec.add(new SearchCriteria("record", record, SearchOperation.EQUAL));
		recordAndCurrentSpec.add(new SearchCriteria("current", true, SearchOperation.EQUAL));
		Page<Version> allCurrentVersions = versionRepository.findAll(recordAndCurrentSpec, PageRequest.of(0, 10));
		assertThat(allCurrentVersions).hasSize(1);
	}

	@Test
	public void testSpecificationJoin() {
		// given a record
		Record record = TestHelper.mockRecord();
		record.setVisible(true);
		entityManager.persistAndFlush(record);

		// with a current version
		Version version = TestHelper.mockVersion(record);
		version.setCurrent(true);
		entityManager.persistAndFlush(version);

		// and a superseded version
		Version superseded = TestHelper.mockVersion(record);
		superseded.setCurrent(false);
		entityManager.persistAndFlush(superseded);

		// given a record
		Record privateRecord = TestHelper.mockRecord();
		privateRecord.setVisible(false);
		entityManager.persistAndFlush(privateRecord);

		// current version on private record
		Version privateVersion = TestHelper.mockVersion(privateRecord);
		privateVersion.setCurrent(true);
		entityManager.persistAndFlush(privateVersion);

		// find all current version for public record should return 1
		VersionSpecification spec = new VersionSpecification();
		spec.add(new SearchCriteria("current", true, SearchOperation.EQUAL));
		spec.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		Page<Version> results = versionRepository.findAll(spec, PageRequest.of(0, 10));
		assertThat(results.getContent()).hasSize(1);
	}

}