package ardc.cerium.core.common.dto;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.mapper.RecordMapper;
import ardc.cerium.core.common.entity.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RecordMapper.class, ModelMapper.class })
public class RecordMapperTest {

	@Autowired
	RecordMapper mapper;

	@Test
	void convertToEntity() {
		RecordDTO dto = new RecordDTO();
		dto.setAllocationID(UUID.randomUUID());
		dto.setOwnerType(Record.OwnerType.User);
		dto.setOwnerID(UUID.randomUUID());
		dto.setCreatedAt(new Date());
		dto.setModifiedAt(new Date());
		dto.setModifierID(UUID.randomUUID());
		dto.setCreatedAt(new Date());

		Record actual = mapper.convertToEntity(dto);
		assertThat(dto.getAllocationID()).isEqualTo(actual.getAllocationID());
		assertThat(dto.getOwnerType()).isEqualTo(actual.getOwnerType());
		assertThat(dto.getOwnerID()).isEqualTo(actual.getOwnerID());
		assertThat(dto.getCreatorID()).isEqualTo(actual.getCreatorID());
		assertThat(dto.getModifiedAt()).isEqualTo(actual.getModifiedAt());
		assertThat(dto.getModifierID()).isEqualTo(actual.getModifierID());
		assertThat(dto.getCreatedAt()).isEqualTo(actual.getCreatedAt());
	}

	@Test
	void convertToDTO() {
		Record record = TestHelper.mockRecord();

		RecordDTO dto = mapper.convertToDTO(record);
		assertThat(dto.getId()).isEqualTo(record.getId());
		assertThat(dto.getAllocationID()).isEqualTo(record.getAllocationID());
		assertThat(dto.getOwnerType()).isEqualTo(record.getOwnerType());
		assertThat(dto.getOwnerID()).isEqualTo(record.getOwnerID());
		assertThat(dto.getCreatorID()).isEqualTo(record.getCreatorID());
		assertThat(dto.getModifiedAt()).isEqualTo(record.getModifiedAt());
		assertThat(dto.getModifierID()).isEqualTo(record.getModifierID());
		assertThat(dto.getCreatedAt()).isEqualTo(record.getCreatedAt());
		assertThat(dto.isVisible()).isEqualTo(record.isVisible());
	}

}