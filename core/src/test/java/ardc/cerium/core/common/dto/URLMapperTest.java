package ardc.cerium.core.common.dto;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.mapper.URLMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.URL;
import ardc.cerium.core.common.service.RecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { URLMapper.class, ModelMapper.class })
class URLMapperTest {

	@Autowired
	URLMapper mapper;

	@MockBean
	RecordService recordService;

	@Test
	void convertToEntity() {
		URLDTO dto = new URLDTO();
		dto.setResolvable(true);
		dto.setUrl("https://researchdata.edu.au");

		URL entity = mapper.convertToEntity(dto);
		assertThat(entity.isResolvable()).isTrue();
		assertThat(entity.getUrl()).isEqualTo(dto.getUrl());
	}

	@Test
	void convertToDTO() {
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		URL entity = TestHelper.mockUrl(record);
		when(recordService.findById(entity.getRecord().getId().toString())).thenReturn(entity.getRecord());

		URLDTO dto = mapper.convertToDTO(entity);
		assertThat(dto.getId()).isEqualTo(entity.getId());
		assertThat(dto.isResolvable()).isEqualTo(entity.isResolvable());
		assertThat(dto.getRecord()).isEqualTo(entity.getRecord().getId());
	}

}