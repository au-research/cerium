package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.specs.SearchCriteria;
import ardc.cerium.core.common.repository.specs.SearchOperation;
import ardc.cerium.core.exception.*;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.VersionRepository;
import ardc.cerium.core.common.repository.specs.VersionSpecification;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VersionService.class, SchemaService.class, VersionMapper.class, ModelMapper.class })
public class VersionServiceTest {

	@Autowired
	VersionService versionService;

	@MockBean
	RecordService recordService;

	@MockBean
	VersionRepository versionRepository;

	@MockBean
	RecordRepository recordRepository;

	@MockBean
	ValidationService validationService;

	@Test
	@DisplayName("search calls repository.findAll")
	void search() {
		versionService.search(new VersionSpecification(), PageRequest.of(0, 10));
		verify(versionRepository, times(1)).findAll(any(VersionSpecification.class), any(Pageable.class));
	}

	@Test
	@DisplayName("findPublicById throws exception when version is not found or record is not visible")
	void findPublicById_throwsException() {
		// throws exception when version doesn't exist
		when(versionRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
		Assert.assertThrows(VersionNotFoundException.class,
				() -> versionService.findPublicById(UUID.randomUUID().toString()));

		// throws exception when version exists but record is not visible
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setVisible(false);
		Version version = TestHelper.mockVersion(record);
		when(versionRepository.findById(any(UUID.class))).thenReturn(Optional.of(version));
		Assert.assertThrows(VersionNotFoundException.class,
				() -> versionService.findPublicById(UUID.randomUUID().toString()));
	}

	@Test
	@DisplayName("findPublicById returns a version when record is visible")
	void findPublicById_visibleRecord_returnsVersion() {
		// get version correctly
		Record expectedRecord = TestHelper.mockRecord(UUID.randomUUID());
		expectedRecord.setVisible(true);
		Version expectedVersion = TestHelper.mockVersion(expectedRecord);
		expectedRecord.setId(UUID.randomUUID());
		when(versionRepository.findById(any(UUID.class))).thenReturn(Optional.of(expectedVersion));
		Version actualVersion = versionService.findPublicById(expectedRecord.getId().toString());
		assertThat(actualVersion).isNotNull();
		assertThat(actualVersion).isInstanceOf(Version.class);
	}

	@Test
	@DisplayName("findVersionForRecord calls findFirstByRecordAndSchemaAndCurrentIsTrue")
	void findVersionForRecord() {
		versionService.findVersionForRecord(TestHelper.mockRecord(), SchemaService.ARDCv1);
		verify(versionRepository, times(1)).findFirstByRecordAndSchemaAndCurrentIsTrue(any(Record.class),
				eq(SchemaService.ARDCv1));
	}

	@Test
	@DisplayName("save calls saveAndFlush")
	void save() {
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Version version = TestHelper.mockVersion(record);
		version.setCreatedAt(null);
		when(versionRepository.saveAndFlush(any(Version.class))).thenReturn(version);

		versionService.save(TestHelper.mockVersion());
		verify(versionRepository, times(1)).saveAndFlush(any(Version.class));
	}

	@Test
	@DisplayName("when creating with a valid request, a DTO is returned properly")
	public void create_ValidRequest_returnsDTO() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup repository mock
		Version expected = TestHelper.mockVersion(record.getId());
		when(recordService.exists(anyString())).thenReturn(true);
		when(recordService.findById(anyString())).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
		when(versionRepository.save(any(Version.class))).thenReturn(expected);

		// when the service creates the version, verify the save method is called
		Version actual = versionService.create(dto, user);
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Version.class);
		verify(versionRepository, times(1)).save(any(Version.class));
	}

	@Test
	public void create_HashAlreadyExists_throwsException() {
		// given a creation request
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup repository mock
		Version expected = TestHelper.mockVersion(record.getId());
		when(recordService.exists(anyString())).thenReturn(true);
		when(recordService.findById(anyString())).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
		when(versionRepository.save(any(Version.class))).thenReturn(expected);
		when(versionRepository.existsBySchemaAndHashAndCurrent(anyString(), anyString(), anyBoolean()))
				.thenReturn(true);

		// throws ForbiddenOpereationException if repository has
		// existsBySchemaAndHashAndCurrent
		Assert.assertThrows(VersionContentAlreadyExistsException.class, () -> versionService.create(dto, user));
	}

	@Test
	public void create_InvalidSchema_throwsException() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema("not-supported");
		dto.setContent("blah");

		// when the service creates the version, expects exception
		Assert.assertThrows(SchemaNotSupportedException.class, () -> versionService.create(dto, user));
	}

	@Test
	public void create_RecordNotFound_throwsException() {
		// given a version dto
		VersionDTO dto = new VersionDTO();
		dto.setRecord(UUID.randomUUID().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		when(recordService.exists(dto.getId())).thenReturn(false);

		// when the service creates the version, expects exception
		Assert.assertThrows(RecordNotFoundException.class, () -> versionService.create(dto, TestHelper.mockUser()));
	}

	@Test
	void create_failValidateRecordOwnership_throwsException() {
		// given a user & record that doesn't belong to that user
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());

		// given a version dto for that record
		VersionDTO dto = new VersionDTO();
		dto.setRecord(record.getId().toString());
		dto.setSchema(SchemaService.IGSNDESCv1);
		dto.setContent("blah");

		// setup the world
		when(recordService.exists(record.getId().toString())).thenReturn(true);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

		Assert.assertThrows(ForbiddenOperationException.class, () -> versionService.create(dto, user));
	}

	@Test
	void delete_VersionNotFound_throwException() {
		Assert.assertThrows(VersionNotFoundException.class,
				() -> versionService.delete(UUID.randomUUID().toString(), TestHelper.mockUser()));
	}

	@Test
	void delete_VersionNotOwned_throwsException() {
		// given a record + version
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());
		Version version = TestHelper.mockVersion(record.getId());

		// mock repository
		when(versionRepository.existsById(version.getId())).thenReturn(true);
		when(versionRepository.findById(version.getId())).thenReturn(Optional.of(version));
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

		// throws ForbiddenOperationException
		Assert.assertThrows(ForbiddenOperationException.class,
				() -> versionService.delete(version.getId().toString(), TestHelper.mockUser()));
	}

	@Test
	void delete_ValidRequest_returnsTrue() {
		// given a record + version
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());
		Version version = TestHelper.mockVersion(record.getId());

		// mock repository
		when(versionRepository.existsById(version.getId())).thenReturn(true);
		when(versionRepository.findById(version.getId())).thenReturn(Optional.of(version));
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);

		// when delete, repository.deleteById is called
		versionService.delete(version.getId().toString(), TestHelper.mockUser());
		verify(versionRepository, times(1)).deleteById(version.getId().toString());
	}

	@Test
	void getHash_ValidVersion_returnsHash() {
		Version version = TestHelper.mockVersion();
		version.setContent("random".getBytes());
		String actual = VersionService.getHash(version);
		assertThat(actual).isEqualTo(DigestUtils.sha1Hex("random"));
	}

	// todo update
	// todo end
	// todo findById
	// todo findOwned

	@Test
	public void end() {
		Version version = TestHelper.mockVersion();
		User user = TestHelper.mockUser();
		when(versionRepository.save(any(Version.class))).thenReturn(version);

		Version endedVersion = versionService.end(version, user);

		// ensure the repository call save
		verify(versionRepository, times(1)).save(version);

		assertThat(endedVersion.isCurrent()).isFalse();
		assertThat(endedVersion.getEndedAt()).isNotNull();
		assertThat(endedVersion.getEndedAt()).isInstanceOf(Date.class);
		assertThat(endedVersion.getEndedBy()).isEqualTo(user.getId());
	}

	@Test
	public void it_can_find_version_by_id() {
		UUID id = UUID.randomUUID();
		Version version = TestHelper.mockVersion(id);
		when(versionRepository.findById(id)).thenReturn(Optional.of(version));

		Version actual = versionService.findById(id.toString());

		// ensure repository call findById
		verify(versionRepository, times(1)).findById(any(UUID.class));

		assertThat(actual).isInstanceOf(Version.class);
	}

	@Test
	public void it_can_find_version_existence_by_id() {
		UUID id = UUID.randomUUID();
		when(versionRepository.existsById(id)).thenReturn(true);

		assertThat(versionService.exists(id.toString())).isTrue();

		// ensure repository call findById
		verify(versionRepository, times(1)).existsById(any(UUID.class));

		// false case

		assertThat(versionService.exists(UUID.randomUUID().toString())).isFalse();
	}

	@Test
	public void it_can_find_versions_existence_by_schema_and_visible_record() throws ParseException {

		String from = "2019-06-25";
		String until = "2020-10-28";

		Date fromDate = convertDate(from);
		Date untilDate = convertDate(until);

		for (int i = 0; i < 10; i++) {
			Record record = TestHelper.mockRecord(UUID.randomUUID());
			record.setVisible(true);
			record.setModifiedAt(fromDate);
			recordRepository.saveAndFlush(record);

			String created = "2019-07-25";
			Date createdAtDate = convertDate(created);
			System.out.println(createdAtDate);
			Version version = TestHelper.mockVersion(record);
			version.setSchema(SchemaService.ARDCv1);
			version.setCreatedAt(createdAtDate);
			version.setCurrent(true);
			versionRepository.saveAndFlush(version);
		}

		// ensure repository call findAllCurrentVersionsOfSchema

		VersionSpecification specs = new VersionSpecification();
		specs.add(new SearchCriteria("schema", SchemaService.ARDCv1, SearchOperation.EQUAL));
		specs.add(new SearchCriteria("visible", true, SearchOperation.RECORD_EQUAL));
		specs.add(new SearchCriteria("createdAt", fromDate, SearchOperation.DATE_GREATER_THAN_EQUAL));
		// specs.add(new SearchCriteria("createdAt", untilDate,
		// SearchOperation.DATE_LESS_THAN_EQUAL));
		Page<Version> versions = versionService.search(specs, PageRequest.of(0, 5));
		System.out.print(versions);
		Page<Version> actual = versionService.findAllCurrentVersionsOfSchema(SchemaService.ARDCv1, fromDate, null,
				PageRequest.of(0, 5));
		System.out.print(actual);
		// is a valid Page<Version>
		// Assertions.assertThat(actual.getContent()).hasSize(1);
		// Assertions.assertThat(actual.getTotalElements()).isEqualTo(1);
		// Assertions.assertThat(actual.getTotalPages()).isEqualTo(1);

		assertThat(versionService.exists(UUID.randomUUID().toString())).isFalse();

	}

	public Date convertDate(String inputDate) {

		try {
			if (inputDate.indexOf('T') > 0) {
				LocalDateTime parsedDate = LocalDateTime.parse(inputDate, DateTimeFormatter.ISO_DATE_TIME);
				Date out = Date.from(parsedDate.atZone(ZoneId.of("UTC")).toInstant());
				return out;
			}
			else {
				LocalDateTime parsedDate = LocalDate.parse(inputDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
				Date out = Date.from(parsedDate.atZone(ZoneId.of("UTC")).toInstant());
				return out;
			}
		}
		catch (Exception e) {
			return null;
		}
	}

}