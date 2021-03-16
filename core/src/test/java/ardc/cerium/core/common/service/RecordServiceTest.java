package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.RecordRepository;
import ardc.cerium.core.common.repository.specs.RecordSpecification;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RecordService.class, RecordMapper.class, ModelMapper.class })
public class RecordServiceTest {

	@MockBean
	ValidationService validationService;

	@Autowired
	private RecordService service;

	@MockBean
	private RecordRepository repository;

	@Test
	@DisplayName("search will call repository.findAll(specs, pageable)")
	void search() {
		service.search(new RecordSpecification(), PageRequest.of(0, 10));
		verify(repository, times(1)).findAll(any(RecordSpecification.class), any(Pageable.class));
	}

	@Test
	@DisplayName("findOwnedById given an existed record with correct validation should return record")
	void findOwnedById_recordExists_returnRecord() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// mock repository
		when(repository.findById(record.getId())).thenReturn(Optional.of(record));
		when(validationService.validateRecordOwnership(record, user)).thenReturn(true);

		// when findOwnedById
		Record actual = service.findOwnedById(record.getId().toString(), user);

		// returns the same record
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Record.class);
		assertThat(actual.getId()).isEqualTo(record.getId());
	}

	@Test
	@DisplayName("Throws Exception when findOwnedById ownership validation failed")
	void findOwnedById_recordNotOwned_throwsException() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// mock repository
		when(repository.findById(record.getId())).thenReturn(Optional.of(record));
		when(validationService.validateRecordOwnership(record, user)).thenReturn(false);

		// when findOwnedById
		Assert.assertThrows(ForbiddenOperationException.class,
				() -> service.findOwnedById(record.getId().toString(), user));
	}

	@Test
	@DisplayName("Saving record calls repository.saveAndFlush")
	void save() {
		service.save(TestHelper.mockRecord());
		verify(repository, times(1)).saveAndFlush(any(Record.class));
	}

	@Test
	@DisplayName("findById throws Exception when record doesn't exist")
	public void findById_recordDoesNotExist_ExceptionRecordNotFound() {
		// given a record & a user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// mock repository
		when(repository.findById(record.getId())).thenReturn(Optional.empty());

		// when findById expects RecordNotFoundException
		Assertions.assertThrows(RecordNotFoundException.class,
				() -> service.findOwnedById(record.getId().toString(), user));
	}

	@Test
	@DisplayName("exists calls repository.existsById")
	public void exists_recordExist_returnTrue() {
		UUID randomUUID = UUID.randomUUID();
		when(repository.existsById(randomUUID)).thenReturn(true);
		assertThat(service.exists(randomUUID.toString())).isTrue();
	}

	@Test
	@DisplayName("exists returns false when record doesn't exist")
	public void exists_recordDoesNotExist_returnFalse() {
		UUID randomUUID = UUID.randomUUID();
		when(repository.existsById(randomUUID)).thenReturn(false);
		assertThat(service.exists(randomUUID.toString())).isFalse();
	}

	@Test
	@DisplayName("create throws exception when user doesn't have access to allocation")
	public void create_UserDoesNotHaveAccessToAllocation_throwsException() {
		// given a User with the wrong permission
		User user = TestHelper.mockUser();

		// given a dto
		RecordDTO dto = new RecordDTO();

		// expects ForbiddenOperationException
		Assertions.assertThrows(ForbiddenOperationException.class, () -> service.create(dto, user));
	}

	@Test
	@DisplayName("create throws exception when user has access to allocation but not the right scope")
	public void create_UserDoesNotHaveScope_throwsException() {
		// given a User with the wrong scope
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Collections.singletonList(Scope.UPDATE));
		user.setAllocations(Collections.singletonList(allocation));

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(allocation.getId());

		// expects ForbiddenOperationException
		Assertions.assertThrows(ForbiddenOperationException.class, () -> service.create(dto, user));
	}

	@Test
	@DisplayName("create returns correct record when user has sufficient permissions")
	public void create_UserHasSufficientPermission_returnsRecord() {
		// given a User with the right permission
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Collections.singletonList(Scope.CREATE));
		user.setAllocations(Collections.singletonList(allocation));

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(allocation.getId());

		// setup repository mock
		Record expected = TestHelper.mockRecord(UUID.randomUUID());
		when(repository.save(any(Record.class))).thenReturn(expected);
		when(validationService.validateAllocationScope(any(Allocation.class), eq(user), eq(Scope.CREATE)))
				.thenReturn(true);

		// when the service creates the record with the dto and the user
		Record result = service.create(dto, user);

		// dto exists and repository.save is called
		assertThat(result).isInstanceOf(Record.class);
		assertThat(result.getId()).isNotNull();
		verify(repository, times(1)).save(any(Record.class));
	}

	@Test
	@DisplayName("create record and overwrite correctly when user has elevated import permission")
	void create_UserHasImportPermission_returnsRecordOverwritten() {
		// given a User with the right permission
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Collections.singletonList(Scope.CREATE));
		user.setAllocations(Collections.singletonList(allocation));

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(allocation.getId());

		Record expected = TestHelper.mockRecord(UUID.randomUUID());

		when(repository.save(any(Record.class))).thenReturn(expected);
		when(validationService.validateAllocationScope(any(Allocation.class), eq(user), eq(Scope.CREATE)))
				.thenReturn(true);
		when(validationService.validateAllocationScope(any(Allocation.class), eq(user), eq(Scope.IMPORT)))
				.thenReturn(true);

		Record result = service.create(dto, user);

		assertThat(result).isInstanceOf(Record.class);
		assertThat(result.getId()).isNotNull();
		verify(repository, times(1)).save(any(Record.class));
	}

	@Test
	@DisplayName("delete throws exception when record doesn't exist")
	void delete_NotExist_throwsException() {
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		when(repository.existsById(record.getId())).thenReturn(false);
		Assertions.assertThrows(RecordNotFoundException.class,
				() -> service.delete(UUID.randomUUID().toString(), TestHelper.mockUser()));
	}

	@Test
	@DisplayName("delete throws exception when the user doesn't own the record")
	public void delete_NotOwned_throwsException() {
		// given a User with the wrong permission
		User user = TestHelper.mockUser();

		// given a record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.DataCenter);
		record.setOwnerID(UUID.randomUUID());

		// record exists for deletion
		when(repository.existsById(record.getId())).thenReturn(true);
		when(repository.findById(record.getId())).thenReturn(Optional.of(record));

		// expects ForbiddenOperationException
		Assertions.assertThrows(ForbiddenOperationException.class,
				() -> service.delete(record.getId().toString(), user));
	}

	@Test
	@DisplayName("delete returns true when the user owns the record")
	public void delete_UserSufficientPermission_returnsTrue() {
		// given a User with the wrong permission
		User user = TestHelper.mockUser();

		// given a record with that allocation
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(user.getId());
		record.setOwnerType(Record.OwnerType.User);

		// record exists for deletion
		when(repository.existsById(record.getId())).thenReturn(true);
		when(repository.findById(record.getId())).thenReturn(Optional.of(record));
		when(validationService.validateRecordOwnership(record, user)).thenReturn(true);

		// when delete
		boolean result = service.delete(record.getId().toString(), user);

		assertThat(result).isTrue();
		verify(repository, times(1)).delete(any(Record.class));
	}

	@Test
	@DisplayName("update throws exception when record doesn't exist")
	public void update_RecordDoesNotExist_throwsException() {
		// given a random dto
		RecordDTO dto = new RecordDTO();
		dto.setId(UUID.randomUUID());

		// when update with a random user expects RecordNotFoundException
		Assertions.assertThrows(RecordNotFoundException.class, () -> service.update(dto, TestHelper.mockUser()));
	}

	@Test
	@DisplayName("update throws exception when user doesn't own the record")
	public void update_OwnerTypeUserUserDoesNotHavePermission_throwsException() {
		// an existing record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());

		// given a User with the wrong permission
		User user = TestHelper.mockUser();

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());

		// pass the existence test
		when(repository.existsById(dto.getId())).thenReturn(true);
		when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

		// expects ForbiddenOperationException
		Assertions.assertThrows(ForbiddenOperationException.class, () -> service.update(dto, user));
	}

	@Test
	@DisplayName("update throws exception when record is owned by a datacenter and the user doesn't have that group")
	public void update_OnwerTypeDataCenter_throwsException() {
		// an existing record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.DataCenter);
		record.setOwnerID(UUID.randomUUID());

		// given a User with the wrong permission
		User user = TestHelper.mockUser();

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());

		// pass the exist test
		when(repository.existsById(dto.getId())).thenReturn(true);
		when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));

		// expects ForbiddenOperationException
		Assertions.assertThrows(ForbiddenOperationException.class, () -> service.update(dto, user));
	}

	@Test
	@DisplayName("update returns record when the user owns the record privately")
	public void update_OwnerTypeUser_returnsRecord() {
		// given a User
		User user = TestHelper.mockUser();

		// owns an existing record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(user.getId());

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());

		// make sure when repository.save call returns a mockRecord and the record exists
		when(repository.existsById(dto.getId())).thenReturn(true);
		when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));
		when(repository.save(any(Record.class))).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), eq(user))).thenReturn(true);

		Record expected = service.update(dto, user);
		assertThat(expected).isInstanceOf(Record.class);
		verify(repository, times(1)).save(any(Record.class));
	}

	@Test
	@DisplayName("update returns record when the user owns the record via data center")
	public void update_OwnerTypeDataCenter_returnsDTO() {
		// datacenter
		DataCenter dataCenter = new DataCenter(UUID.randomUUID());

		// given a User
		User user = TestHelper.mockUser();
		user.setDataCenters(Collections.singletonList(dataCenter));

		// owns an existing record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.DataCenter);
		record.setOwnerID(dataCenter.getId());

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());

		// make sure when repository.save call returns a mockRecord and the record exists
		when(repository.existsById(dto.getId())).thenReturn(true);
		when(repository.findById(any(UUID.class))).thenReturn(Optional.of(record));
		when(repository.save(any(Record.class))).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), eq(user))).thenReturn(true);

		Record expected = service.update(dto, user);
		assertThat(expected).isInstanceOf(Record.class);
		verify(repository, times(1)).save(any(Record.class));
	}

	@Test
	@DisplayName("update returns overwritten record when the user has import elevated permission")
	public void update_UserImportScope_returnsRecord() throws ParseException {
		// given a User with the import scope to the record
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Arrays.asList(Scope.UPDATE, Scope.IMPORT));

		// given existing record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(user.getId());
		record.setAllocationID(allocation.getId());

		// given a dto
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());
		dto.setCreatedAt(new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989"));
		dto.setModifiedAt(new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989"));
		dto.setCreatorID(UUID.randomUUID());

		// make sure when repository.save call returns a mockRecord and the record exists
		when(repository.existsById(dto.getId())).thenReturn(true);
		when(repository.findById(dto.getId())).thenReturn(Optional.of(record));
		when(repository.save(any(Record.class))).thenReturn(TestHelper.mockRecord());
		when(validationService.validateRecordOwnership(any(Record.class), eq(user))).thenReturn(true);
		when(validationService.validateAllocationScope(any(Allocation.class), eq(user), eq(Scope.IMPORT)))
				.thenReturn(true);

		Record expected = service.update(dto, user);
		assertThat(expected).isInstanceOf(Record.class);
		verify(repository, times(1)).save(any(Record.class));
	}

	@Test
	@DisplayName("findPublicById throws exception when record doesn't exist")
	void findPublicById_notfound_throwsException() {
		// given no record
		when(repository.findById(anyString())).thenReturn(null);

		// when findPublicById, throws Exception
		Assert.assertThrows(RecordNotFoundException.class, () -> service.findPublicById(UUID.randomUUID().toString()));
	}

	@Test
	@DisplayName("findPublicById throws exception when record is not visible")
	void findPublicById_privateRecord_throwsException() {
		// given a private record
		Record record = TestHelper.mockRecord();
		record.setVisible(false);
		when(repository.findById(anyString())).thenReturn(Optional.of(record));

		// when findPublicById, throws Exception
		Assert.assertThrows(RecordNotFoundException.class, () -> service.findPublicById(UUID.randomUUID().toString()));
	}

	@Test
	@DisplayName("findPublicById return record when record is public")
	void findPublicById_foundRecord_returns() {
		// given a public record
		Record expected = TestHelper.mockRecord(UUID.randomUUID());
		expected.setVisible(true);
		when(repository.findById(expected.getId())).thenReturn(Optional.of(expected));

		// when findPublicById, returns a Record
		Record actual = service.findPublicById(expected.getId().toString());
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Record.class);
	}

}