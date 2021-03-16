package ardc.cerium.core.common.dto;

import ardc.cerium.core.TestHelper;
import ardc.cerium.core.common.dto.mapper.IdentifierMapper;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
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
@ContextConfiguration(classes = { IdentifierMapper.class, ModelMapper.class })
class IdentifierMapperTest {

	@Autowired
	IdentifierMapper mapper;

	@MockBean
	RecordService recordService;

	@Test
	void convertToEntity() {
		// given a record
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		when(recordService.findById(record.getId().toString())).thenReturn(record);

		// given a dto
		IdentifierDTO dto = new IdentifierDTO();
		dto.setType(Identifier.Type.IGSN);
		dto.setValue("10.7531/XXAA998");
		dto.setRecord(record.getId());

		// converts to entity correctly, carries the record
		Identifier entity = mapper.convertToEntity(dto);
		assertThat(entity.getType()).isEqualTo(Identifier.Type.IGSN);
		assertThat(entity.getValue()).isEqualTo("10.7531/XXAA998");
		assertThat(entity.getRecord()).isInstanceOf(Record.class);
		assertThat(entity.getRecord().getId()).isEqualTo(record.getId());
	}

	@Test
	void convertToDTO() {
		// given an entity
		Record record = TestHelper.mockRecord(UUID.randomUUID());
		Identifier entity = TestHelper.mockIdentifier(record);

		// converts to dto correctly
		IdentifierDTO dto = mapper.convertToDTO(entity);
		assertThat(dto.getType()).isEqualTo(entity.getType());
		assertThat(dto.getValue()).isEqualTo(entity.getValue());
		assertThat(dto.getRecord()).isInstanceOf(UUID.class);
		assertThat(dto.getRecord()).isEqualTo(record.getId());
	}

}