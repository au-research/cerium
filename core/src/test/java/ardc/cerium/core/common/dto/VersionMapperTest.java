package ardc.cerium.core.common.dto;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.mapper.VersionMapper;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.RecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { VersionMapper.class, ModelMapper.class })
class VersionMapperTest {

	@Autowired
	VersionMapper mapper;

	@MockBean
	RecordService recordService;

	@Test
	void convertToEntity() {
		// given a dto
		VersionDTO dto = new VersionDTO();
		dto.setSchema("test-schema");
		dto.setRecord(UUID.randomUUID().toString());
		dto.setContent(Base64.getEncoder().encodeToString("stuff".getBytes()));

		// converts to entity correctly
		Version actual = mapper.convertToEntity(dto);
		assertThat(actual.getSchema()).isEqualTo(dto.getSchema());
		assertThat(actual.getRecord()).isNull();
		assertThat(actual.getContent()).isNotEmpty();
		assertThat(new String(actual.getContent(), StandardCharsets.UTF_8)).isEqualTo("stuff");
	}

	@Test
	void convertToDto() {
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Version entity = TestHelper.mockVersion(UUID.randomUUID());
		entity.setRecord(record);
		when(recordService.findById(entity.getRecord().getId().toString())).thenReturn(entity.getRecord());

		VersionDTO dto = mapper.convertToDTO(entity);
		assertThat(dto.getId()).isEqualTo(entity.getId().toString());
		assertThat(dto.getSchema()).isEqualTo(entity.getSchema());
		assertThat(dto.getRecord()).isEqualTo(record.getId().toString());
	}

}