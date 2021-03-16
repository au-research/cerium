package ardc.cerium.oai.service;

import ardc.cerium.core.common.config.ApplicationProperties;
import ardc.cerium.core.common.entity.Record;
import ardc.cerium.core.common.entity.Version;
import ardc.cerium.core.common.model.Schema;
import ardc.cerium.core.common.provider.Metadata;
import ardc.cerium.core.common.provider.MetadataProviderFactory;
import ardc.cerium.core.common.service.RecordService;
import ardc.cerium.core.common.service.SchemaService;
import ardc.cerium.core.common.service.VersionService;
import ardc.cerium.core.common.util.Helpers;
import ardc.cerium.oai.exception.*;
import ardc.cerium.oai.model.*;
import ardc.cerium.oai.provider.OAIProvider;
import ardc.cerium.oai.response.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.oai.enabled")
public class OAIPMHService {

	public static final int pageSize = 100;

	public static enum Verb {

		IDENTIFY("Identify"), GETRECORD("GetRecord"), LISTRECORDS("ListRecords"), LISTIDENTIFIERS(
				"ListIdentifiers"), LISTSETS("ListSets"), LISTMETADATAFORMATS("ListMetadataFormats");

		private String inputVerb;

		Verb(String verb) {
			this.inputVerb = verb;
		}

		public String getVerb() {
			return this.inputVerb;
		}

	}

	@Autowired
	RecordService recordService;

	@Autowired
	VersionService versionService;

	@Autowired
	ApplicationProperties applicationProperties;

	@Autowired
	SchemaService schemaService;

	public OAIResponse identify() {
		IdentifyFragment identify = new IdentifyFragment();
		identify.setRepositoryName(applicationProperties.getName());
		identify.setBaseURL(applicationProperties.getUrl());
		identify.setProtocolVersion("2.0");
		identify.setAdminEmail(applicationProperties.getContactEmail());
		identify.setEarliestDatestamp(recordService.findEarliest());
		identify.setDeletedRecord("transient");
		identify.setGranularity("YYYY-MM-DDThh:mm:ssZ");
		return new OAIIdentifyResponse(identify);
	}

	public OAIResponse getRecord(String metadataPrefix, String identifier) {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		if (identifier == null) {
			throw new BadArgumentException();
		}

		try {
			Record record = recordService.findPublicById(identifier);
			Version version = versionService.findVersionForRecord(record, metadataPrefix);
			String content = new String(version.getContent());
			return new GetRecordResponse(record, content);
		}
		catch (Exception e) {
			throw new IdDoesNotExistException();
		}
	}

	public OAIResponse listRecords(String metadataPrefix, String resumptionToken, String from, String until)
			throws JsonProcessingException {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		Date fromDate = Helpers.convertDate(from);
		if (fromDate == null && from != null)
			throw new BadArgumentException();

		Date untilDate = Helpers.convertDate(until);
		if (untilDate == null && until != null)
			throw new BadArgumentException();

		long cursor = OAIProvider.getCursor(resumptionToken, pageSize);
		String newResumptionToken;
		newResumptionToken = OAIProvider.getResumptionToken(resumptionToken, pageSize);
		Pageable pageable = OAIProvider.getPageable(resumptionToken, pageSize);
		try {
			Page<Version> versions = versionService.findAllCurrentVersionsOfSchema(metadataPrefix, fromDate, untilDate,
					pageable);
			if (!versions.hasContent())
				throw new NoRecordsMatchException();
			OAIListRecordsResponse response = new OAIListRecordsResponse();
			ListRecordsFragment listRecordsFragment = new ListRecordsFragment();

			for (Version version : versions) {
				Record record = version.getRecord();
				String content = new String(version.getContent());
				RecordFragment recordFragment = new RecordFragment(record, content);
				listRecordsFragment.setListRecords(recordFragment);
			}
			response.setRecordsFragment(listRecordsFragment);
			if (!versions.isLast() && versions.hasContent()) {
				ResumptionTokenFragment resumptionTokenFragment = new ResumptionTokenFragment();
				resumptionTokenFragment.setToken(String.valueOf(versions.getTotalElements()), String.valueOf(cursor),
						newResumptionToken);
				listRecordsFragment.setResumptionTokenFragmentFragment(resumptionTokenFragment);
			}
			return response;
		}
		catch (Exception e) {
			throw new NoRecordsMatchException();
		}

	}

	public OAIResponse listIdentifiers(String metadataPrefix, String resumptionToken, String from, String until)
			throws JsonProcessingException {
		if (metadataPrefix == null) {
			throw new BadArgumentException();
		}

		if (!this.isOAIProvider(schemaService.getSchemaByID(metadataPrefix))) {
			throw new CannotDisseminateFormatException();
		}

		Date fromDate = Helpers.convertDate(from);
		if (fromDate == null && from != null)
			throw new BadArgumentException();

		Date untilDate = Helpers.convertDate(until);
		if (untilDate == null && until != null)
			throw new BadArgumentException();

		long cursor = OAIProvider.getCursor(resumptionToken, pageSize);
		String newResumptionToken;
		newResumptionToken = OAIProvider.getResumptionToken(resumptionToken, pageSize);
		Pageable pageable = OAIProvider.getPageable(resumptionToken, pageSize);
		try {
			Page<Version> versions = versionService.findAllCurrentVersionsOfSchema(metadataPrefix, fromDate, untilDate,
					pageable);
			if (!versions.hasContent())
				throw new NoRecordsMatchException();
			OAIListIdentifiersResponse response = new OAIListIdentifiersResponse();
			ListIdentifiersFragment listIdentifiersFragment = new ListIdentifiersFragment();
			for (Version version : versions) {
				Record record = version.getRecord();
				RecordHeaderFragment headerFragment = new RecordHeaderFragment(record.getId().toString(),
						record.getModifiedAt());
				listIdentifiersFragment.setListIdentifiers(headerFragment);
			}
			response.setIdentifiersFragment(listIdentifiersFragment);
			if (!versions.isLast()) {
				ResumptionTokenFragment resumptionTokenFragment = new ResumptionTokenFragment();
				resumptionTokenFragment.setToken(String.valueOf(versions.getTotalElements()), String.valueOf(cursor),
						newResumptionToken);
				listIdentifiersFragment.setResumptionTokenFragmentFragment(resumptionTokenFragment);
			}
			return response;
		}
		catch (Exception e) {
			throw new NoRecordsMatchException();
		}
	}

	public OAIResponse listMetadataFormats() {
		OAIListMetadataFormatsResponse response = new OAIListMetadataFormatsResponse();
		ListMetadataFormatsFragment metadataFormatsFragment = new ListMetadataFormatsFragment();
		List<Schema> schemas = this.getOAIProviders();
		try {
			for (Schema schema : schemas) {
				OAIProvider oaiProvider = (OAIProvider) MetadataProviderFactory.create(schema, Metadata.OAI);
				metadataFormatsFragment.setMetadataFormat(oaiProvider.getPrefix(schema),
						oaiProvider.getFormatSchema(schema), oaiProvider.getNamespace(schema));
			}
			response.setListMetadataFormatsFragment(metadataFormatsFragment);
			return response;
		}
		catch (Exception e) {
			throw new NoMetadataFormatsException();
		}
	}

	public OAIResponse listSets() {
		throw new NoSetHierarchyException();
	}

	/**
	 * Checks if the given schema is available for OAI export
	 * @return List of schemas that are able to export to oai
	 */
	public List<Schema> getOAIProviders() {
		List<Schema> oaiSchemas = new ArrayList<Schema>();
		List<Schema> schemas = schemaService.getSchemas();
		for (Schema schema : schemas) {
			if (this.isOAIProvider(schema)) {
				oaiSchemas.add(schema);
			}
		}
		return oaiSchemas;
	}

	/**
	 * Checks if the given schema is available for OAI export
	 * @param schema the schema to be checked
	 * @return Boolean
	 */
	public Boolean isOAIProvider(Schema schema) {
		try {
			return schema.getProviders().containsKey(Metadata.OAI);
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if the given verb is a valid OAI verb
	 * @param inputVerb the verb to be checked
	 * @return Boolean
	 */
	public Boolean isValidVerb(String inputVerb) {
		for (Verb verb : Verb.values()) {
			if (verb.getVerb().equals(inputVerb))
				return true;
		}
		return false;
	}

}
