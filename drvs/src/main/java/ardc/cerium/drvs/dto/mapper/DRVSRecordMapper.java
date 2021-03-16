package ardc.cerium.drvs.dto.mapper;

import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Allocation;
import ardc.cerium.core.common.model.DataCenter;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.drvs.dto.CollectionValidationSummaryDTO;
import ardc.cerium.drvs.dto.DRVSRecordDTO;
import ardc.cerium.drvs.model.CollectionValidationSummary;
import ardc.cerium.drvs.model.DRVSSubmission;
import ardc.cerium.drvs.service.CollectionValidationService;
import ardc.cerium.drvs.service.DOIHarvestService;
import ardc.cerium.drvs.service.DRVSImportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DRVSRecordMapper {

	final ModelMapper modelMapper;

	final KeycloakService kcService;

	final VersionService versionService;

	protected Converter<Record, DRVSRecordDTO> converter;

	public DRVSRecordMapper(ModelMapper modelMapper, KeycloakService kcService, VersionService versionService) {
		this.modelMapper = modelMapper;
		this.kcService = kcService;
		this.versionService = versionService;
		converter = buildConverter();
	}

	public Converter<Record, DRVSRecordDTO> getConverter() {
		return converter;
	}

	private Converter<Record, DRVSRecordDTO> buildConverter() {
		return new Converter<Record, DRVSRecordDTO>() {
			@Override
			protected DRVSRecordDTO doForward(Record record) {
				DRVSRecordDTO dto = modelMapper.map(record, DRVSRecordDTO.class);

				// the projectOwner is always the first and only DataCenter available in
				// the allocation
				UUID allocationID = record.getAllocationID();
				try {
					Allocation allocation = kcService.getAllocationByResourceID(String.valueOf(allocationID));
					DataCenter projectPartner = allocation.getDataCenters().get(0);
					dto.setProjectPartner(projectPartner);
				}
				catch (Exception e) {
					// todo handle failure to get data center
				}

				// submission
				Version latestDRVSSubmission = versionService.findVersionForRecord(record,
						DRVSImportService.DRVS_SUBMISSION_SCHEMA_ID);
				if (latestDRVSSubmission != null) {
					try {
						DRVSSubmission submission = (new ObjectMapper())
								.readValue(new String(latestDRVSSubmission.getContent()), DRVSSubmission.class);
						dto.setSubmission(submission);
					}
					catch (JsonProcessingException e) {
						// todo handle failure to process the json stored
					}
				}

				// DataCiteXML
				Version latestDataCiteXML = versionService.findVersionForRecord(record, DOIHarvestService.DataCiteXML);
				if (latestDataCiteXML != null) {
					dto.setDataciteXML(new String(latestDataCiteXML.getContent()));
					dto.setHarvestedAt(latestDataCiteXML.getCreatedAt());
				}

				// validation
				Version validationVersion = versionService.findVersionForRecord(record,
						CollectionValidationService.DRVS_CollVS);
				if (validationVersion != null) {
					try {
						CollectionValidationSummary summary = (new ObjectMapper()).readValue(
								new String(validationVersion.getContent()), CollectionValidationSummary.class);
						CollectionValidationSummaryDTO summaryDTO = modelMapper.map(summary,
								CollectionValidationSummaryDTO.class);
						dto.setValidation(summaryDTO);
					}
					catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				else {
					dto.setValidation(new CollectionValidationSummaryDTO());
				}

				return dto;
			}

			@Override
			protected Record doBackward(DRVSRecordDTO drvsRecordDTO) {
				return modelMapper.map(drvsRecordDTO, Record.class);
			}
		};
	}

}
