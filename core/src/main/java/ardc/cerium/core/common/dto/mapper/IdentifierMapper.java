package ardc.cerium.core.common.dto.mapper;

import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.service.RecordService;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class IdentifierMapper {

	final ModelMapper modelMapper;

	final RecordService recordService;

	protected Converter<Identifier, IdentifierDTO> converter;

	public IdentifierMapper(ModelMapper modelMapper, RecordService recordService) {
		this.modelMapper = modelMapper;
		this.recordService = recordService;
		this.converter = buildConverter();
	}

	public Identifier convertToEntity(IdentifierDTO dto) {
		return converter.reverse().convert(dto);
	}

	public IdentifierDTO convertToDTO(Identifier entity) {
		return converter.convert(entity);
	}

	private Converter<Identifier, IdentifierDTO> buildConverter() {
		return new Converter<Identifier, IdentifierDTO>() {
			@Override
			protected IdentifierDTO doForward(Identifier identifier) {
				IdentifierDTO dto = modelMapper.map(identifier, IdentifierDTO.class);
				dto.setRecord(identifier.getRecord().getId());
				return dto;
			}

			@Override
			protected Identifier doBackward(IdentifierDTO identifierDTO) {
				Identifier entity = modelMapper.map(identifierDTO, Identifier.class);
				if (identifierDTO.getRecord() != null) {
					entity.setRecord(recordService.findById(identifierDTO.getRecord().toString()));
				}
				return entity;
			}
		};
	}

	public Converter<Identifier, IdentifierDTO> getConverter() {
		return this.converter;
	}

}
