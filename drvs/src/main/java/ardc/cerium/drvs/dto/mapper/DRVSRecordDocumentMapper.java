package ardc.cerium.drvs.dto.mapper;

import ardc.cerium.core.common.entity.Identifier;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.service.KeycloakService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.drvs.dto.CollectionValidationSummaryDTO;
import ardc.cerium.drvs.dto.DRVSRecordDocument;
import ardc.cerium.drvs.model.CollectionValidationSummary;
import ardc.cerium.drvs.service.CollectionValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class DRVSRecordDocumentMapper {

	final ModelMapper modelMapper;

	final KeycloakService kcService;

	final VersionService versionService;

	protected Converter<Record, DRVSRecordDocument> converter;

	public DRVSRecordDocumentMapper(ModelMapper modelMapper, KeycloakService kcService, VersionService versionService) {
		this.modelMapper = modelMapper;
		this.kcService = kcService;
		this.versionService = versionService;
		converter = buildConverter();
	}

	public Converter<Record, DRVSRecordDocument> getConverter() {
		return converter;
	}

	private Converter<Record, DRVSRecordDocument> buildConverter() {
		return new Converter<Record, DRVSRecordDocument>() {
			@Override
			protected DRVSRecordDocument doForward(Record record) {
				DRVSRecordDocument doc = modelMapper.map(record, DRVSRecordDocument.class);

				// localCollectionID
				record.getIdentifiers().stream().filter(id -> id.getType().equals(Identifier.Type.DRVS)).findFirst()
						.ifPresent(localCollectionID -> doc.setLocalCollectionID(localCollectionID.getValue()));

				// DOI
				record.getIdentifiers().stream().filter(id -> id.getType().equals(Identifier.Type.DOI)).findFirst()
						.ifPresent(DOI -> doc.setDOI(DOI.getValue()));

				// status & validation
				Version validationVersion = versionService.findVersionForRecord(record,
						CollectionValidationService.DRVS_CollVS);
				if (validationVersion != null) {
					try {
						CollectionValidationSummary summary = (new ObjectMapper()).readValue(
								new String(validationVersion.getContent()), CollectionValidationSummary.class);
						if (summary == null) {
							summary = new CollectionValidationSummary();
						}
						CollectionValidationSummaryDTO summaryDTO = modelMapper.map(summary,
								CollectionValidationSummaryDTO.class);
						doc.setStatus(summaryDTO.getStatus().toString());
						doc.setValidation(summary);
					}
					catch (JsonProcessingException e) {
						e.printStackTrace();
						// todo handle reading summary
					}
				}
				else {
					doc.setStatus(CollectionValidationSummary.Status.UNVALIDATED.toString());
				}

				return doc;
			}

			@Override
			protected Record doBackward(DRVSRecordDocument drvsRecordDocument) {
				return null;
			}
		};
	}

}
