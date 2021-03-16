package ardc.cerium.core.common.dto.mapper;

import ardc.cerium.core.common.dto.VersionDTO;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.RecordService;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Base64;

@Service
public class VersionMapper {

	protected Converter<Version, VersionDTO> converter;

	ModelMapper modelMapper;

	RecordService recordService;

	public VersionMapper(ModelMapper modelMapper, RecordService recordService) {
		this.modelMapper = modelMapper;
		this.recordService = recordService;
	}

	@PostConstruct
	void init() {
		this.converter = buildConverter();
	}

	public Converter<Version, VersionDTO> getConverter() {
		return converter;
	}

	public Version convertToEntity(VersionDTO versionDTO) {
		return this.converter.reverse().convert(versionDTO);
	}

	public VersionDTO convertToDTO(Version version) {
		return this.converter.convert(version);
	}

	private Converter<Version, VersionDTO> buildConverter() {
		return new Converter<Version, VersionDTO>() {
			@Override
			protected VersionDTO doForward(Version version) {
				VersionDTO versionDTO = modelMapper.map(version, VersionDTO.class);
				versionDTO.setRecord(version.getRecord().getId().toString());
				if (versionDTO.getId() == null && version.getId() != null) {
					versionDTO.setId(version.getId().toString());
				}
				versionDTO.setContent(null);
				return versionDTO;
			}

			@Override
			protected Version doBackward(VersionDTO versionDTO) {
				Version version = modelMapper.map(versionDTO, Version.class);
				if (versionDTO.getRecord() != null) {
					version.setRecord(recordService.findById(versionDTO.getRecord()));
				}
				version.setContent(Base64.getDecoder().decode(versionDTO.getContent()));
				return version;
			}
		};
	}

}
