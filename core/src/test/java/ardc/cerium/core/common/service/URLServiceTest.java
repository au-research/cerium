package ardc.cerium.core.common.service;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.URLDTO;
import ardc.cerium.core.common.dto.mapper.URLMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.exception.ForbiddenOperationException;
import ardc.cerium.core.exception.RecordNotFoundException;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.repository.URLRepository;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { URLService.class, URLMapper.class, ModelMapper.class })
public class URLServiceTest {

	@Autowired
	private URLService service;

	@MockBean
	private URLRepository repository;

	@MockBean
	RecordService recordService;

	@MockBean
	ValidationService validationService;

	@Test
	void create_RecordNotFound_throwsException() {
		URLDTO dto = new URLDTO();
		dto.setRecord(UUID.randomUUID());
		dto.setUrl("https://researchdata.edu.au");

		Assert.assertThrows(RecordNotFoundException.class, () -> {
			service.create(dto, TestHelper.mockUser());
		});
	}

	@Test
	void create_Forbidden_throwsException() {
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());
		dto.setUrl("https://researchdata.edu.au");

		// record exists
		when(recordService.exists(record.getId().toString())).thenReturn(true);

		// mockUser does not have the right ownership
		Assert.assertThrows(ForbiddenOperationException.class, () -> {
			service.create(dto, TestHelper.mockUser());
		});
	}

	@Test
	void create_Valid_returnsDTO() {
		// given a user owns a record
		User user = TestHelper.mockUser();
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		record.setOwnerID(user.getId());
		record.setOwnerType(Record.OwnerType.User);

		// and a url dto request
		URLDTO dto = new URLDTO();
		dto.setRecord(record.getId());
		dto.setUrl("https://researchdata.edu.au");

		// setup the world
		when(recordService.exists(record.getId().toString())).thenReturn(true);
		when(recordService.findById(record.getId().toString())).thenReturn(record);
		when(validationService.validateRecordOwnership(record, user)).thenReturn(true);
		when(repository.save(any(URL.class))).thenReturn(TestHelper.mockUrl());

		// when create
		service.create(dto, user);

		// verify save is called
		verify(repository, times(1)).save(any(URL.class));
	}

	@Test
	public void it_can_find_url_by_id() {
		UUID id = UUID.randomUUID();
		URL url = TestHelper.mockUrl(id);
		when(repository.findById(id)).thenReturn(Optional.of(url));

		URL actual = service.findById(id.toString());

		// ensure repository call findById
		verify(repository, times(1)).findById(any(UUID.class));

		assertThat(actual).isInstanceOf(URL.class);
	}

	@Test
	public void it_can_find_url_existence_by_id() {
		UUID id = UUID.randomUUID();
		when(repository.existsById(id)).thenReturn(true);

		assertThat(service.exists(id.toString())).isTrue();

		// ensure repository call findById
		verify(repository, times(1)).existsById(any(UUID.class));

		// false case
		assertThat(service.exists(UUID.randomUUID().toString())).isFalse();
	}

	@Test
	public void it_can_delete_url_by_id() {
		UUID id = UUID.randomUUID();

		service.delete(id.toString());
		// ensure repository call deleteById
		verify(repository, times(1)).deleteById(any(String.class));
	}

	@Test
	public void it_can_create_a_url() {
		URL newUrl = TestHelper.mockUrl();
		service.create(newUrl);
		verify(repository, times(1)).save(newUrl);
	}

	@Test
	public void it_updates_url_correctly() {
		String new_url = "http://changed_url.com";
		URL actual = TestHelper.mockUrl();
		String update_id = actual.getId().toString();

		when(repository.save(any(URL.class))).thenReturn(actual);
		actual.setUrl(new_url);
		URL updated = service.update(actual);

		// the save method is invoked on the repository
		verify(repository, times(1)).save(any(URL.class));
		String compare_id = updated.getId().toString();
		String updated_url = updated.getUrl();

		// the updated url is returned with updated values
		Assertions.assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(actual.getUpdatedAt());
		Assertions.assertThat(update_id.equals(compare_id));
		Assertions.assertThat(new_url.equals(updated_url));
	}

}
