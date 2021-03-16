package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.Scope;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class RecordServiceIT {

	@Autowired
	RecordService service;

	@Autowired
	RecordRepository repository;

	@Autowired
	RecordMapper mapper;

	@Test
	public void findById_recordExists_returnsRecord() {
		User user = TestHelper.mockUser();

		// given a record
		Record expected = TestHelper.mockRecord();
		expected.setOwnerType(Record.OwnerType.User);
		expected.setOwnerID(user.getId());
		repository.saveAndFlush(expected);

		// when findById
		Record actual = service.findOwnedById(expected.getId().toString(), user);

		// found the right record
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Record.class);
		assertThat(actual.getId()).isEqualTo(expected.getId());
	}

	@Test
	public void create_UserSufficientPermission_returnsDTO() {
		// given the user with proper permission
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Arrays.asList(Scope.CREATE));
		user.setAllocations(Arrays.asList(allocation));

		// given the dto
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(allocation.getId());

		// when create
		Record result = service.create(dto, user);
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Record.class);
		assertThat(result.getId()).isNotNull();

		// actualRecord has createdAt, creatorID, ownerType and ownerID
		Record actualRecord = service.findById(result.getId().toString());
		assertThat(actualRecord.getCreatedAt()).isNotNull();
		assertThat(actualRecord.getCreatorID()).isEqualTo(user.getId());
		assertThat(actualRecord.getOwnerType()).isEqualTo(Record.OwnerType.User);
		assertThat(actualRecord.getOwnerID()).isEqualTo(user.getId());
	}

	@Test
	public void create_UserImportScope_returnsNonDefaultDTO() throws ParseException {
		// given the user with proper permission
		User user = TestHelper.mockUser();
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Arrays.asList(Scope.CREATE, Scope.IMPORT));
		user.setAllocations(Arrays.asList(allocation));

		// given the dto
		Date updatedCreatedAt = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
		Date updatedModifiedAt = new SimpleDateFormat("dd/MM/yyyy").parse("03/03/1989");
		UUID updatedCreatorID = UUID.randomUUID();
		UUID updatedOwnerID = UUID.randomUUID();
		UUID updatedDataCenterID = UUID.randomUUID();
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(allocation.getId());
		dto.setModifiedAt(updatedModifiedAt);
		dto.setCreatedAt(updatedCreatedAt);
		dto.setCreatorID(updatedCreatorID);
		dto.setOwnerID(updatedOwnerID);

		// when create
		Record result = service.create(dto, user);
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Record.class);
		assertThat(result.getId()).isNotNull();

		// actualRecord has createdAt, creatorID, ownerType and ownerID
		Record actualRecord = service.findById(result.getId().toString());
		assertThat(actualRecord.getCreatedAt()).isEqualTo(updatedCreatedAt);
		assertThat(actualRecord.getModifiedAt()).isEqualTo(updatedModifiedAt);
		assertThat(actualRecord.getCreatorID()).isEqualTo(updatedCreatorID);
		assertThat(actualRecord.getOwnerID()).isEqualTo(updatedOwnerID);
	}

	@Test
	public void update_UserSufficientPermission_returnsDTO() {
		// given the user with proper permission
		User user = TestHelper.mockUser();

		// given a record
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(user.getId());
		record.setVisible(true);
		record.setCreatedAt(new Date());
		record = repository.saveAndFlush(record);

		// when update with a dto to change
		RecordDTO dto = new RecordDTO();
		dto.setId(record.getId());
		dto.setVisible(false);

		// when update with the modified object
		Record result = service.update(dto, user);

		// the result dto is the same as the object
		assertThat(result).isInstanceOf(Record.class);

		// record is updated with the new status
		Record actual = service.findById(record.getId().toString());
		assertThat(actual).isNotNull();
		assertThat(actual.isVisible()).isFalse();
	}

	@Test
	public void update_UserImportScope_returnsOverwrittenDTO() throws ParseException {

		// given the user
		Allocation allocation = new Allocation(UUID.randomUUID());
		allocation.setScopes(Arrays.asList(Scope.UPDATE, Scope.IMPORT));
		User user = TestHelper.mockUser();
		user.setAllocations(Arrays.asList(allocation));

		// given a record
		Record record = TestHelper.mockRecord();
		record.setOwnerType(Record.OwnerType.User);
		record.setAllocationID(allocation.getId());
		record.setOwnerID(user.getId());
		record.setCreatedAt(new Date());
		record = repository.saveAndFlush(record);

		// the update payload contains new fields
		Date updatedCreatedAt = new SimpleDateFormat("dd/MM/yyyy").parse("02/02/1989");
		Date updatedModifiedAt = new SimpleDateFormat("dd/MM/yyyy").parse("03/03/1989");
		UUID updatedOwnerID = UUID.randomUUID();
		UUID updatedCreatorID = UUID.randomUUID();
		RecordDTO dto = mapper.convertToDTO(record);
		dto.setModifiedAt(updatedModifiedAt);
		dto.setCreatedAt(updatedCreatedAt);
		dto.setCreatorID(updatedCreatorID);
		dto.setOwnerID(updatedOwnerID);

		// when update
		Record result = service.update(dto, user);

		// the result contains the updated fields with no exception
		assertThat(result).isNotNull();
		assertThat(result.getCreatedAt()).isEqualTo(updatedCreatedAt);
		assertThat(result.getModifiedAt()).isEqualTo(updatedModifiedAt);
		assertThat(result.getCreatorID()).isEqualTo(updatedCreatorID);
		assertThat(result.getOwnerID()).isEqualTo(updatedOwnerID);

		// and the result record in the database contains the updated fields
		Record actualRecord = service.findById(record.getId().toString());
		assertThat(actualRecord.getCreatedAt()).isEqualTo(updatedCreatedAt);
		assertThat(actualRecord.getModifiedAt()).isEqualTo(updatedModifiedAt);
		assertThat(actualRecord.getCreatorID()).isEqualTo(updatedCreatorID);
	}

	@Test
	public void exists_recordExists_returnsTrue() {
		// random uuid doesn't exist
		assertThat(service.exists(UUID.randomUUID().toString())).isFalse();

		// when a record is created
		Record record = repository.save(new Record());

		// it exists
		assertThat(service.exists(record.getId().toString())).isTrue();
	}

	@Test
	public void delete_UserSufficientPermission_returnsTrue() {

		// given a user with update scope to that allocation
		User user = TestHelper.mockUser();

		// given a record that has that allocation
		Record record = new Record();
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(user.getId());
		record.setCreatedAt(new Date());
		record = repository.save(record);

		// (sanity check) that record exists
		assertThat(service.exists(record.getId().toString())).isTrue();

		// when delete
		boolean result = service.delete(record.getId().toString(), user);

		// it returns true and it's gone
		assertThat(result).isTrue();
		assertThat(service.exists(record.getId().toString())).isFalse();
	}

}
