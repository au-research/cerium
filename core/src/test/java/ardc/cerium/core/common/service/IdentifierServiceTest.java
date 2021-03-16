package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.IdentifierRepository;
import ardc.cerium.core.common.repository.specs.IdentifierSpecification;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { IdentifierService.class, IdentifierMapper.class, ModelMapper.class })
class IdentifierServiceTest {

	@MockBean
	ValidationService validationService;

	@Autowired
	private IdentifierService service;

	@MockBean
	private RecordService recordService;

	@MockBean
	private IdentifierRepository repository;

	@Test
	void findById_IdentifierFound_returnsIdentifier() {
		UUID id = UUID.randomUUID();
		Identifier identifier = TestHelper.mockIdentifier(id);
		when(repository.findById(id)).thenReturn(Optional.of(identifier));

		Identifier actual = service.findById(id.toString());

		// ensure repository call findById
		verify(repository, times(1)).findById(any(UUID.class));
		assertThat(actual).isNotNull();
		assertThat(actual).isInstanceOf(Identifier.class);
	}

	@Test
	void findById_IdentifierNotFound_returnsNull() {
		Identifier actual = service.findById(UUID.randomUUID().toString());

		// ensure repository call findById
		verify(repository, times(1)).findById(any(UUID.class));
		assertThat(actual).isNull();
	}

	@Test
	void exists_callsExistsByID_returnsTrue() {
		UUID id = UUID.randomUUID();
		when(repository.existsById(id)).thenReturn(true);

		assertThat(service.exists(id.toString())).isTrue();

		// ensure repository call existsById
		verify(repository, times(1)).existsById(any(UUID.class));

		// false case
		assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
	}

	@Test
	void create_RecordNotFound_throwsException() {
		// given a version dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(UUID.randomUUID());

		when(recordService.exists(dto.getRecord().toString())).thenReturn(false);

		// when the service creates the version, expects exception
		Assert.assertThrows(RecordNotFoundException.class, () -> {
			service.create(dto, TestHelper.mockUser());
		});
	}

	@Test
	void create_failValidateRecordOwnership_throwsException() {
		// given a user & record that doesn't belong to that user
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerType(Record.OwnerType.User);
		record.setOwnerID(UUID.randomUUID());

		// given a version dto for that record
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());

		// setup the world
		when(recordService.exists(record.getId().toString())).thenReturn(true);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(false);

		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			service.create(dto, user);
		});
	}

	@Test
	void create_ValidRequest_returnsDTO() {
		// given a record & user
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		User user = TestHelper.mockUser();

		// given a version dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setRecord(record.getId());
		dto.setType(Identifier.Type.IGSN);
		dto.setValue("IGSNVALUE");

		// setup repository mock
		Identifier expected = TestHelper.mockIdentifier(record);
		when(recordService.exists(anyString())).thenReturn(true);
		when(recordService.findById(anyString())).thenReturn(record);
		when(validationService.validateRecordOwnership(any(Record.class), any(User.class))).thenReturn(true);
		when(repository.save(any(Identifier.class))).thenReturn(expected);

		// when the service creates the version, verify the save method is called
		Identifier result = service.create(dto, user);
		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(Identifier.class);
		verify(repository, times(1)).save(any(Identifier.class));
	}

	@Test
	void delete_call_repository() {
		UUID id = UUID.randomUUID();

		service.delete(id.toString());
		// ensure repository call deleteById
		verify(repository, times(1)).deleteById(any(UUID.class));
	}

	@Test
	void search_validSearchSpec_returnsPageOfDTO() {
		// given a page of 2 Identifier (as result)
		// and repository.findAll(spec, page) returns a page of 2 Identifier
		Page<Identifier> mockPage = new PageImpl(
				Arrays.asList(TestHelper.mockIdentifier(), TestHelper.mockIdentifier()));
		when(repository.findAll(any(IdentifierSpecification.class), any(Pageable.class))).thenReturn(mockPage);

		// when service.search
		Page<Identifier> result = service.search(new IdentifierSpecification(), PageRequest.of(0, 2));

		// the result is a Page of Identifier
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		verify(repository, times(1)).findAll(any(IdentifierSpecification.class), any(Pageable.class));
	}

}
