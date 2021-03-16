package ardc.cerium.drvs.dto.mapper;

import ardc.cerium.core.common.entity.Request;
import ardc.cerium.core.common.model.Attribute;
import ardc.cerium.core.common.model.User;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.drvs.dto.DRVSRequestDTO;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DRVSRequestMapper {

	final ModelMapper modelMapper;

	final KeycloakService keycloakService;

	protected Converter<Request, DRVSRequestDTO> converter;

	public DRVSRequestMapper(ModelMapper modelMapper, KeycloakService keycloakService) {
		this.modelMapper = modelMapper;
		this.keycloakService = keycloakService;
		converter = buildConverter();
	}

	public Converter<Request, DRVSRequestDTO> getConverter() {
		return converter;
	}

	private Converter<Request, DRVSRequestDTO> buildConverter() {
		return new Converter<Request, DRVSRequestDTO>() {
			@Override
			protected DRVSRequestDTO doForward(Request request) {
				DRVSRequestDTO dto = modelMapper.map(request, DRVSRequestDTO.class);
				Map<String, String> summary = new HashMap<>();
				if(request.getType().contains("harvest")){
					summary.put("new_metadata_harvested", getRequestValue(request, Attribute.NUM_OF_RECORDS_CREATED));
					summary.put("metadata_updated", getRequestValue(request, Attribute.NUM_OF_RECORDS_UPDATED));
					summary.put("metadata_unchanged", getRequestValue(request, Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED));
					summary.put("harvest_failed", getRequestValue(request, Attribute.NUM_OF_ERROR));
					summary.put("dois_to_harvest", getRequestValue(request, Attribute.NUM_OF_RECORDS_RECEIVED));
				}
				else{
					summary.put("created", getRequestValue(request, Attribute.NUM_OF_RECORDS_CREATED));
					summary.put("updated", getRequestValue(request, Attribute.NUM_OF_RECORDS_UPDATED));
					summary.put("unchanged", getRequestValue(request, Attribute.NUM_OF_RECORD_CONTENT_NOT_CHANGED));
					summary.put("errored", getRequestValue(request, Attribute.NUM_OF_ERROR));
					summary.put("received", getRequestValue(request, Attribute.NUM_OF_RECORDS_RECEIVED));
				}
				dto.setSummary(summary);

				try {
					User creator = keycloakService.getUserByUUID(request.getCreatedBy());
					dto.setCreator(creator);
				} catch (Exception e) {
					// handle creating creator
				}

				return dto;
			}

			@Override
			protected Request doBackward(DRVSRequestDTO drvsRequestDTO) {
				return modelMapper.map(drvsRequestDTO, Request.class);
			}
		};
	}

	/**
	 * Due to limited structuring of attributes, we have to resort to this to comfortably
	 * display 0 when attributes is not found
	 * @param request the {@link Request} to obtain attribute from
	 * @param attribute the {@link Attribute} to obtain the value from
	 * @return the String value of the attribute
	 */
	private String getRequestValue(Request request, Attribute attribute) {
		return Optional.ofNullable(request.getAttribute(attribute)).orElse("0");
	}

}
