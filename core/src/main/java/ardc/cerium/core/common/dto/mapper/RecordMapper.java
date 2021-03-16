package ardc.cerium.core.common.dto.mapper;

import ardc.cerium.core.common.dto.IdentifierDTO;
import ardc.cerium.core.common.dto.RecordDTO;
import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.entity.Record;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordMapper {

	final ModelMapper modelMapper;

	protected Converter<Record, RecordDTO> converter;

	public RecordMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	@PostConstruct
	public void init() {
		this.converter = buildConverter();
	}

	public Record convertToEntity(RecordDTO dto) {
		return this.converter.reverse().convert(dto);
	}

	public RecordDTO convertToDTO(Record record) {
		return this.converter.convert(record);
	}

	public Converter<Record, RecordDTO> getConverter() {
		return converter;
	}

	private Converter<Record, RecordDTO> buildConverter() {
		return new Converter<Record, RecordDTO>() {
			@Override
			protected RecordDTO doForward(Record record) {
				RecordDTO dto = modelMapper.map(record, RecordDTO.class);
				List<IdentifierDTO> identifiers = record.getIdentifiers().stream()
						.map(identifier -> modelMapper.map(identifier, IdentifierDTO.class))
						.collect(Collectors.toList());
				dto.setIdentifiers(identifiers);

				List<VersionDTO> currentVersions = record.getCurrentVersions().stream()
						.map(version -> modelMapper.map(version, VersionDTO.class))
						.peek(versionDTO -> versionDTO.setContent(null)).peek(versionDTO -> versionDTO.setRecord(null))
						.collect(Collectors.toList());
				dto.setCurrentVersions(currentVersions);
				return dto;
			}

			@Override
			protected Record doBackward(RecordDTO recordDTO) {
				return modelMapper.map(recordDTO, Record.class);
			}
		};
	}

}
